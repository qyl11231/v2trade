CREATE TABLE 'kline_15m' (
	symbol SYMBOL CAPACITY 256 CACHE,
	ts TIMESTAMP,
	open DOUBLE,
	high DOUBLE,
	low DOUBLE,
	close DOUBLE,
	volume DOUBLE,
	exchange_ts LONG,
	source_kline_count INT
) timestamp(ts) PARTITION BY DAY WAL
WITH maxUncommittedRows=500000, o3MaxLag=600000000us;


CREATE TABLE 'kline_1h' (
	symbol SYMBOL CAPACITY 256 CACHE,
	ts TIMESTAMP,
	open DOUBLE,
	high DOUBLE,
	low DOUBLE,
	close DOUBLE,
	volume DOUBLE,
	exchange_ts LONG,
	source_kline_count INT
) timestamp(ts) PARTITION BY DAY WAL
WITH maxUncommittedRows=500000, o3MaxLag=600000000us;



CREATE TABLE 'kline_1m' (
	symbol SYMBOL CAPACITY 256 CACHE,
	ts TIMESTAMP,
	open DOUBLE,
	high DOUBLE,
	low DOUBLE,
	close DOUBLE,
	volume DOUBLE,
	exchange_ts LONG
) timestamp(ts) PARTITION BY DAY WAL
WITH maxUncommittedRows=500000, o3MaxLag=600000000us
DEDUP UPSERT KEYS(symbol,ts);



CREATE TABLE 'kline_2h' (
	symbol SYMBOL CAPACITY 256 CACHE,
	ts TIMESTAMP,
	open DOUBLE,
	high DOUBLE,
	low DOUBLE,
	close DOUBLE,
	volume DOUBLE,
	exchange_ts LONG,
	source_kline_count INT
) timestamp(ts) PARTITION BY DAY WAL
WITH maxUncommittedRows=500000, o3MaxLag=600000000us;


CREATE TABLE 'kline_30m' (
	symbol SYMBOL CAPACITY 256 CACHE,
	ts TIMESTAMP,
	open DOUBLE,
	high DOUBLE,
	low DOUBLE,
	close DOUBLE,
	volume DOUBLE,
	exchange_ts LONG,
	source_kline_count INT
) timestamp(ts) PARTITION BY DAY WAL
WITH maxUncommittedRows=500000, o3MaxLag=600000000us;



CREATE TABLE 'kline_4h' (
	symbol SYMBOL CAPACITY 256 CACHE,
	ts TIMESTAMP,
	open DOUBLE,
	high DOUBLE,
	low DOUBLE,
	close DOUBLE,
	volume DOUBLE,
	exchange_ts LONG,
	source_kline_count INT
) timestamp(ts) PARTITION BY DAY WAL
WITH maxUncommittedRows=500000, o3MaxLag=600000000us;


CREATE TABLE 'kline_5m' (
	symbol SYMBOL CAPACITY 256 CACHE,
	ts TIMESTAMP,
	open DOUBLE,
	high DOUBLE,
	low DOUBLE,
	close DOUBLE,
	volume DOUBLE,
	exchange_ts LONG,
	source_kline_count INT
) timestamp(ts) PARTITION BY DAY WAL
WITH maxUncommittedRows=500000, o3MaxLag=600000000us;