package com.ins.seckill.service.Impl;

import com.ins.seckill.dao.SeckillDao;
import com.ins.seckill.dao.SucceedKilledDao;
import com.ins.seckill.dao.cache.RedisDao;
import com.ins.seckill.dto.Exposer;
import com.ins.seckill.dto.SeckillExecution;
import com.ins.seckill.entity.Seckill;
import com.ins.seckill.entity.SucceedKilled;
import com.ins.seckill.enums.SeckillStatusEnum;
import com.ins.seckill.exception.RepeatKillException;
import com.ins.seckill.exception.SeckillCloseException;
import com.ins.seckill.exception.SeckillException;
import com.ins.seckill.service.SeckillService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;

/**
 * The type Seckill service.
 *
 * @author blue
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SucceedKilledDao succeedKilledDao;

    @Autowired
    private RedisDao redisDao;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private final String salt = "safas978dsfgs%djkZ^bgv$sudiyggsd&*6%^3!#";

    @Override
    public List<Seckill> listSeckill() {
        return seckillDao.queryAll(0, 10);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        //使用 Redis 进行缓存优化：在超时的基础上维护一致性
        //在缓存寻找秒杀商品的数据
        Seckill seckill = redisDao.getSeckill(seckillId);

        if (seckill == null) {
            // 如果缓存中为空，则进入数据库查询
            seckill = seckillDao.queryById(seckillId);
            //如果数据库为空，说明秒杀对象不存在
            if (seckill == null) {
                return new Exposer(false, seckillId);
            } else {
                redisDao.putSeckill(seckill);
            }
        }

        Date now = new Date();
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        //如果现在时间小于秒杀开启时间，或大于秒杀结束时间，那么就不暴露秒杀地址
        if (now.getTime() < startTime.getTime() || now.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId, now.getTime(), startTime.getTime(), endTime.getTime());
        }
        //todo:生成随机盐值
        //todo:将生成的md5直接放入缓存中，跳过判断和生成md5这一步，有效时间设为结束时间减开始时间
        String md5 = getMD5(seckillId);
        return new Exposer(true, seckillId, md5);
    }

    /**
     * 执行秒杀操作，如果成功，返回成功信息和成功记录
     * 如果失败，则抛出异常
     */
    @Override
    @Transactional
    public SeckillExecution executionSeckill(long seckillId, long userId, String md5) throws SeckillException {
        if (md5 == null || !getMD5(seckillId).equals(md5)) {
            throw new RepeatKillException("数据被篡改");
        }

        Date now = new Date();
        //先插入购买记录，再减少库存，可以有效减少 update 过程中行级锁的持有时间。
        try {
            // todo:优化为查询redis中是否有记录，如果没有，则将消息发往消息队列
            String record = seckillId + ":" + userId;
            if (redisDao.existSeckillRecord(record)) {
                throw new RepeatKillException("重复秒杀");
            }
            // todo:增加消费者
            amqpTemplate.convertAndSend("SeckillRecord", record);
            redisDao.putSeckillRecord(record);


            //减库存
            int reduceNum = seckillDao.reduceNumber(seckillId, now);
            if (reduceNum <= 0) {
                throw new SeckillCloseException("秒杀已结束");
            } else {
                //秒杀成功
                SucceedKilled succeedKilled = succeedKilledDao.queryByIdWithSeckill(seckillId, userId);
                return new SeckillExecution(seckillId, SeckillStatusEnum.SUCCESS, succeedKilled);
            }
        } catch (SeckillCloseException | RepeatKillException e) {
            throw e;
        } catch (Exception e) {
            //将所有编译期间异常转化为运行时异常，可以使 Spring 声明式事务进行回滚
            throw new SeckillException("秒杀内部错误" + e.getMessage());
        }
    }

    /**
     * 生成一个 md5 的值
     *
     * @param seckillId
     * @return md5 字符串
     */
    private String getMD5(long seckillId) {
        String base = seckillId + "/" + salt;
        return DigestUtils.md5DigestAsHex(base.getBytes());
    }
}
