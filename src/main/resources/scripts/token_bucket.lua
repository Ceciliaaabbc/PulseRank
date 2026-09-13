-- 分布式令牌桶：把桶状态存在 Redis Hash 里(tokens, ts)，用 Lua 脚本保证
-- "读取剩余令牌 -> 按时间差补充令牌 -> 扣减 -> 写回" 这一整套操作在 Redis
-- 里原子执行，多个应用实例并发调用不会读到中间态、也不会重复放行。
-- KEYS[1] = 桶的 key
-- ARGV[1] = capacity      桶容量
-- ARGV[2] = refillPerSec  每秒补充的令牌数
-- ARGV[3] = now           当前时间(毫秒)
-- ARGV[4] = requested     本次请求消耗的令牌数(固定传1)
local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(bucket[1])
local ts = tonumber(bucket[2])
local capacity = tonumber(ARGV[1])
local refillPerSec = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

if tokens == nil then
    tokens = capacity
    ts = now
end

local elapsedMs = math.max(0, now - ts)
local refill = elapsedMs * refillPerSec / 1000.0
tokens = math.min(capacity, tokens + refill)

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('HMSET', KEYS[1], 'tokens', tostring(tokens), 'ts', tostring(now))
redis.call('EXPIRE', KEYS[1], 60)

return {allowed, tokens}
