package io.kischang.fun.tvm3u.controller;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.common.HeaderValue;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.server.annotation.RequestPath;
import org.tio.http.server.util.Resps;
import org.tio.utils.http.HttpUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 程序核心功能
 */
@RequestPath
public class MainController {

	private static Logger log = LoggerFactory.getLogger(MainController.class);

    public static String TV_M3U_CONTENT = null;
    private static OkHttpClient okHttpClient  = new OkHttpClient.Builder()
			//设置超时
			.connectTimeout(3000, TimeUnit.SECONDS)
			.writeTimeout(3000, TimeUnit.SECONDS)
			.readTimeout(3000, TimeUnit.SECONDS)
			.build();

    //从这个源获取基础的直播地址列表
    private static final String SOURCE_M3U = "http://kudian.xyz/tv.m3u";


    private static final String EPG_URL = "http://epg.51zmt.top:8000/upload/";

    @RequestPath(value = "/main/re_gen")
    public HttpResponse re_gen(HttpRequest request) throws Exception {
    	TV_M3U_CONTENT = null;
		return Resps.html(request, "操作成功，已清除缓存，重新访问即可。");
	}

    @RequestPath(value = "/main/tv.m3u")
    public HttpResponse main(HttpRequest request) throws Exception {
        if (TV_M3U_CONTENT == null) {
			return returnM3U(request,
					reGetAndCache()
			);
        }else {
			return returnM3U(request, TV_M3U_CONTENT);
		}
    }

	public static String reGetAndCache() throws Exception {
		//执行更新处理
		Response resp = HttpUtils.get(SOURCE_M3U);
		//基础数据
		String tmp = "#EXTINF:-1 group-title=\"基础频道-源获取失败\",CCTV-1\nhttp://cctvcnch5ca.v.wscdns.com/live/cctv1_2/10793.m3u8\n";
		if (resp.isSuccessful()) {
			//获取成功了
			tmp = resp.body().string();
		}
		//内部解析一次
		tmp = parseM3uSourceToEpg(tmp);
		//解析指南数据
		String rv = parseEpg(tmp);
		if (null == rv){
			//返回一个临时版数据
			return tmp;
		}else {
			//再反向恢复分组信息
			rv = parseM3uEpgToSource(rv);
			TV_M3U_CONTENT = rv;
			return rv;
		}
	}

	private static String parseEpg(String tmp) {
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart(
						"myfile"
						, "tv.m3u"
						, RequestBody.create(tmp, MediaType.parse("application/octet-stream"))
				)
				.build();

		Request uploadReq = new Request.Builder()
				.url(EPG_URL)
				.post(requestBody)
				.build();
		try {
			Response epgResp = okHttpClient.newCall(uploadReq)
					.execute();
			if (epgResp.isSuccessful()){
				//下载指南解析后的数据
				String rvBody = epgResp.body().string();
				if (rvBody.contains("匹配成功")){
					String lastM3uUrl = rvBody.substring(rvBody.indexOf("<a href=") + 9, rvBody.indexOf(".m3u") + 4);
					Response resp = HttpUtils.get(lastM3uUrl);
					if (resp.isSuccessful()) {
						//获取成功了
						return resp.body().string();
					}
				}
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//返回临时版
		return null;
	}

	private HttpResponse returnM3U(HttpRequest request, String tmp) {
		return Resps.string(request
				, tmp
				, "gb2312"
				, HeaderValue.Content_Type.TEXT_PLAIN_TXT.value
		);
	}


	private static String parseM3uEpgToSource(String m3uSource) {
		String state_Group = null;
		StringBuilder m3uBuilder = new StringBuilder();
		for (String onceLine : m3uSource.split("\n")){
			onceLine = onceLine.trim();
			if (isNullString(onceLine)){
				m3uBuilder.append("\n");
			}else if (onceLine.startsWith("#EXTM3U")){
				m3uBuilder.append("#EXTM3U\n");
			}else {
				if (onceLine.startsWith("#EXTINF:")){
					if (onceLine.contains("group-title")){
						//更新分组信息
						int gt = onceLine.indexOf("group-title") + 10;
						int start = onceLine.indexOf("\"", gt) + 1;
						state_Group = onceLine.substring(start, onceLine.indexOf("\"", start));
						//多加一行空格分隔
						m3uBuilder.append("\n");
					}
					String tvName = onceLine.substring(onceLine.lastIndexOf(",") + 1);
					if (!isNullString(tvName)){
						tvName = tvNameMap.getOrDefault(tvName, tvName);
						m3uBuilder.append(onceLine, 0, onceLine.indexOf("group-title="))
								.append(" group-title=\"")
								.append(tvGroupMap.getOrDefault(tvName, state_Group))
								.append("\",")
								.append(tvName)
								.append("\n");
					}
				}else {
					m3uBuilder.append(onceLine).append("\n");
				}
			}
		}
		return m3uBuilder.toString();
	}

	private static final Map<String, String> tvNameMap = new ConcurrentHashMap<>();
	private static final Map<String, String> tvGroupMap = new ConcurrentHashMap<>();

	//处理原始列表，便于指南进行解析
	private static String parseM3uSourceToEpg(String m3uSource) {
		String state_Group = null;
		StringBuilder m3uBuilder = new StringBuilder();
		for (String onceLine : m3uSource.split("\n")){
			onceLine = onceLine.trim();
			if (isNullString(onceLine)){
				m3uBuilder.append("\n");
			}else if (onceLine.startsWith("#EXTM3U")){
				//忽略行
			}else {
				if (onceLine.startsWith("#EXTINF:")){
					if (onceLine.contains("group-title")){
						//更新分组信息
						int gt = onceLine.indexOf("group-title") + 10;
						int start = onceLine.indexOf("\"", gt) + 1;
						state_Group = onceLine.substring(start, onceLine.indexOf("\"", start));
						//多加一行空格分隔
						m3uBuilder.append("\n");
					}
					String tvName = onceLine.substring(onceLine.lastIndexOf(",") + 1);
					if (!isNullString(tvName)){
						///忽略
						/*m3uBuilder.append("#EXTINF:-1 group-title=\"")
								.append(state_Group)
								.append("\",")
								.append(tvName)
								.append("\n")*/

						//Egp解析不能太复杂
						String tvNameTar = tvName + genRandStr();
						tvNameMap.put(tvNameTar, tvName);//本地缓存映射关系
						tvGroupMap.put(tvName, state_Group);//本地缓存所属分组
						m3uBuilder.append(tvNameTar).append("\n");
						;
					}
				}else {
					m3uBuilder.append(onceLine).append("\n");
				}
			}
		}
		return m3uBuilder.toString();
	}

	private static String genRandStr() {
		String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}


	private static boolean isNullString(String line) {
		return line == null || "".equals(line);
	}


    @RequestPath(value = "/err_500")
    public HttpResponse errPage(HttpRequest request) throws Exception {
        return Resps.html(request, "出错了，请稍后再试");
    }

}
