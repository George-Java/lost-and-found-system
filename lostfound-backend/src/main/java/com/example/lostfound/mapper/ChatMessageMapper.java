package com.example.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lostfound.domain.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/** 私信表 Mapper：会话查询在 ChatMessageService（继承 ServiceImpl）。 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
