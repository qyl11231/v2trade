rules 下字段定义 
    rules_type 大脑类型
        1.趋势
        2.马丁
        3.网格
    
    entryRules 入场交易规则 
       entryType 趋势类型
                1.同时满足配置条件    all
                2.满足权重的可以买入  weight
            
       factorList  因子集合
            factor 因子对象
                factorType 因子类型
                    1.price 价格
                    2.singal 信号
                    3.indicator 指标
                factorType = price类型时
                    factor {
                        factorType :  price,
                        params:{ 
                            kSizeHigh: 60 //60跟k线最高价
                            kSizelow: 60 //60跟k线最低价
                        }                        
                        condition {
                           highFlag == currentPrice >  kSizeHigh  = true
                            lowFlag == currentPrice < kSizelow = true
                        }
                        educt {
                            highFlag : true /false
                            lowFlag : true /false
                        }
                        scope{
                            highFlagWeight :20
                            lowFlagWeight :-20
                        }
                    }


                factorType = singal类型时
                    factor {
                            factorType :  singal,
                            params:{ 
                                validityPeriod: 60 信号有效期
                                increase: 0.03 涨幅 
                                decline: 0.03 跌幅
                            }                        
                            condition {
                                if(singaAction  == LONG){
                                    （currentPrice - signaPrice） >  decline == true     
                                }
                                  if(singaAction  == SHORT){
                                    （signaPrice - currentPrice） >  increase == true     
                                }
                                
                            }
                            educt {
                                declineFlag : true /false
                                increaseFlag : true /false
                            }
                            scope{
                                declineWeight :20
                                increaseWeight :-20
                            }
                         }


                factorType = indicator 类型时

                        factor {
                            factorType :  indicator,
                            indicatorCode: "rsi"
                            params:{
                                xxx长度:14
                        
                            }                        
                            condition {
                              longflag =  value < 30
                              shortFlag =  value > 80
                                
                            }
                            educt {
                                longflag : true /false
                                shortFlag : true /false
                            }
                            scope{
                                longWeight :20
                                shortWeight :-20
                            }
                         }

        exit {

            


        }