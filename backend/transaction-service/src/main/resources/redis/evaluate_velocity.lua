-- ADR-0030 velocity guard: ZSET 10m/24h per-user + daily amount counter (ponytail: 10m/24h windows, 5 tx/10m + daily amount threshold, in-memory Redis only)
local userKey10m = KEYS[1]
local userKey24h = KEYS[2]
local amountKey = KEYS[3]
local now = tonumber(ARGV[1])
local amount = tonumber(ARGV[2])
local window10m = 600
local window24h = 86400
redis.call('ZREMRANGEBYSCORE', userKey10m, 0, now - window10m)
redis.call('ZREMRANGEBYSCORE', userKey24h, 0, now - window24h)
local count10m = redis.call('ZCARD', userKey10m)
local count24h = redis.call('ZCARD', userKey24h)
local dailyAmount = tonumber(redis.call('GET', amountKey) or "0")
if count10m >= 5 then return {429, count10m, count24h, dailyAmount} end
if dailyAmount + amount > 50000000 then return {429, count10m, count24h, dailyAmount} end
redis.call('ZADD', userKey10m, now, now .. ':' .. amount)
redis.call('ZADD', userKey24h, now, now .. ':' .. amount)
redis.call('EXPIRE', userKey10m, window10m)
redis.call('EXPIRE', userKey24h, window24h)
redis.call('INCRBYFLOAT', amountKey, amount)
redis.call('EXPIRE', amountKey, window24h)
return {200, count10m + 1, count24h + 1, dailyAmount + amount}
