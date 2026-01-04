package com.qyl.v2trade;

import com.qyl.v2trade.common.util.UtcTimeConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class V2TradeApplicationTests {

    @Test
    void contextLoads() {

        //https://www.okx.com/api/v5/market/candles?instId=BTC-USDT-SWAP&bar=1m&after=1767052800000&before=1767056400000
        //https://www.okx.com/api/v5/market/candles?instId=BTC-USDT-SWAP&bar=1m&before=1767052800000&after=1767056400000
    }
//1767222840000l

    public static void main(String[] args) {
        String utc = UtcTimeConverter.utcTimestampToUtcString(1767239640000l);
        System.out.printf("utc="+utc);
        String utc1 = UtcTimeConverter.utcTimestampToUtcString(1767251640000l);
        System.out.println();
        System.out.println("utc="+utc1);
    }


}
