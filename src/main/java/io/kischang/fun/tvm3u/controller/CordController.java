package io.kischang.fun.tvm3u.controller;

import okhttp3.Response;
import org.tio.http.common.HeaderName;
import org.tio.http.common.HeaderValue;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.server.annotation.RequestPath;
import org.tio.http.server.util.Resps;
import org.tio.utils.http.HttpUtils;
import org.tio.utils.hutool.FileUtil;

import java.util.Base64;

/**
 * @author KisChang
 * @date 2020-03-25
 */
@RequestPath(value = "/cc")
public class CordController {

    private static final String BASEURL = "https://www.";


    @RequestPath(value = "/main")
    public HttpResponse main(String base, String key, int[] arg, boolean en, HttpRequest request) throws Exception {
        StringBuilder rv = new StringBuilder();
        for (int mu : arg) {
            Response resp = HttpUtils.get(BASEURL + base + "/link/" + key + "?mu=" + mu);
            if (resp.isSuccessful()) {//获取成功了
                String once = new String(Base64.getDecoder().decode(resp.body().string()));
                rv.append(once);
            }
        }
        String fileName = key + ".txt";
        byte[] rvBytes = rv.toString().getBytes();
        if (en){
            //是否base64加密
            rvBytes = Base64.getEncoder().encode(rvBytes);
        }

        HttpResponse resp = Resps.bytesWithContentType(request
                , rvBytes
                , "multipart/form-data");
        resp.addHeader(HeaderName.Content_Disposition, HeaderValue.from(new String(
                ("attachment;fileName=" + fileName).getBytes()
                , "ISO8859-1")));
        return resp;
    }

}
