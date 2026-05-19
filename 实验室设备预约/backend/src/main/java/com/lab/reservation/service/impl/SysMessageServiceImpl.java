package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.SysMessage;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.SysMessageMapper;
import com.lab.reservation.mapper.SysUserMapper;
import com.lab.reservation.service.SysMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统消息服务实现类
 */
@Service
public class SysMessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements SysMessageService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    @Transactional
    public void sendMessage(Long userId, String type, String title, String content, Long relatedId, String relatedType) {
        SysMessage message = new SysMessage();
        message.setUserId(userId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(0);
        message.setRelatedId(relatedId);
        message.setRelatedType(relatedType);
        baseMapper.insert(message);
    }

    @Override
    public Page<SysMessage> pageMyMessages(Long userId, Integer pageNum, Integer pageSize, String type, Integer isRead,
                                           String startDate, String endDate) {
        Page<SysMessage> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("deleted", 0);

        if (type != null && !type.isEmpty()) {
            wrapper.eq("type", type);
        }
        if (isRead != null) {
            wrapper.eq("is_read", isRead);
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            wrapper.ge("create_time", startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            wrapper.le("create_time", endDate.trim() + " 23:59:59");
        }

        // 待读优先排序，然后按创建时间倒序
        wrapper.orderByDesc("is_read", "create_time");

        Page<SysMessage> result = baseMapper.selectPage(page, wrapper);

        // 填充用户名信息
        for (SysMessage msg : result.getRecords()) {
            if (msg.getUserId() != null) {
                SysUser user = sysUserMapper.selectById(msg.getUserId());
                if (user != null) {
                    msg.setUsername(user.getUsername());
                    msg.setRealName(user.getRealName());
                }
            }
        }

        return result;
    }

    @Override
    public int getUnreadCount(Long userId) {
        return baseMapper.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public boolean markAsRead(Long messageId, Long userId) {
        SysMessage message = baseMapper.selectById(messageId);
        if (message == null || message.getDeleted() != 0) {
            return false;
        }
        // 校验归属
        if (!message.getUserId().equals(userId)) {
            return false;
        }
        if (message.getIsRead() != null && message.getIsRead() == 1) {
            return true; // 已经是已读状态
        }
        message.setIsRead(1);
        baseMapper.updateById(message);
        return true;
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        baseMapper.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional
    public boolean deleteMessage(Long messageId, Long userId) {
        SysMessage message = baseMapper.selectById(messageId);
        if (message == null || message.getDeleted() != 0) {
            return false;
        }
        // 校验归属
        if (!message.getUserId().equals(userId)) {
            return false;
        }
        baseMapper.deleteById(messageId);
        return true;
    }
}
