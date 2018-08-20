package com.line.pay.chatbot.service;

import com.google.gson.Gson;
import com.line.pay.chatbot.payment.ReserveRequest;
import com.line.pay.chatbot.payment.ReserveResponse;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LinePayService {
    private static Logger logger= LogManager.getLogger(LineMessageService.class.getName());

    @Value("${line.pay.channel.id}")
    private String channelId;

    @Value("${line.pay.channel.secret}")
    private String channelSecret;

    @Value("${line.pay.api.url}")
    private String payApiUrl;

    @Value("${line.pay.api.reserve.url}")
    private String payApiReserveUrl;

    @Value("${line.pay.confirm.url}")
    private String confirmUrl;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public ReserveResponse invokeReserve(long amount) {
        ReserveRequest reserveRequest = getReserveRequest(amount);

        var gson = new Gson();

        var json = gson.toJson(reserveRequest);
        logger.info("Reserve request:" + json);

        ReserveResponse reserveResponse = getReserveResponse(gson, json);

        return reserveResponse;
    }

    public void invokeConfirm(String orderId) throws Exception{

        var url = payApiUrl + "/" + orderId + "confirm";
        var client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).execute();

    }

    public ReserveRequest getReserveRequest(long amount) {
        var reserveRequest = new ReserveRequest();

        var orderId = UUID.randomUUID().toString();

        reserveRequest.setAmount(amount);
        reserveRequest.setCapture("true");
        reserveRequest.setCheckConfirmUrlBrowser("false");
        reserveRequest.setConfirmUrl(confirmUrl + orderId);
        reserveRequest.setConfirmUrlType("CLIENT");
        reserveRequest.setCurrency("TWD");
        reserveRequest.setOrderId(orderId);
        reserveRequest.setPayType("NORMAL");
        reserveRequest.setProductImageUrl("testUrl");
        reserveRequest.setProductName("TEST");
        return reserveRequest;
    }

    public ReserveResponse getReserveResponse(Gson gson, String json) {
        RequestBody body = RequestBody.create(JSON, json);

        var url = payApiUrl + payApiReserveUrl;

        var client = new OkHttpClient();

        Request request = buildLinePayRequest(body, url);

        var reserveResponse = new ReserveResponse();

        try {
            Response response = client.newCall(request).execute();
            logger.info("Response HTTP Status:" + response.code());

            var responseBody = response.body().string();

            response.close();

            reserveResponse = gson.fromJson(responseBody, ReserveResponse.class);

        } catch (Exception e) {
            logger.error(e);
        }
        return reserveResponse;
    }

    public Request buildLinePayRequest(RequestBody body, String url) {
        return new Request.Builder()
                    .addHeader("X-LINE-ChannelId", channelId)
                    .addHeader("X-LINE-ChannelSecret", channelSecret)
                    .url(url)
                    .post(body)
                    .build();
    }
}