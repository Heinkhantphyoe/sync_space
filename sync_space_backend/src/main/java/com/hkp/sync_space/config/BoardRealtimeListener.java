package com.hkp.sync_space.config;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hkp.sync_space.board.BoardChanged;
import com.hkp.sync_space.board.BoardEvent;

@Component
public class BoardRealtimeListener {

	private final SimpMessagingTemplate messagingTemplate;

	public BoardRealtimeListener(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onBoardChanged(BoardChanged event) {
		messagingTemplate.convertAndSend("/topic/spaces/" + event.spaceId(),
				new BoardEvent(event.type(), event.board()));
	}

}
