package com.hkp.sync_space.board;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.hkp.sync_space.space.Space;
import com.hkp.sync_space.user.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_activity")
@Getter
@Setter
@NoArgsConstructor
public class TaskActivity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false)
	private Space space;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_id", nullable = false)
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TaskActivityKind kind;

	@Column(length = 2000)
	private String body;

	@Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
	private Instant createdAt;

	public TaskActivity(Space space, Task task, User actor, TaskActivityKind kind, String body, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.space = space;
		this.task = task;
		this.actor = actor;
		this.kind = kind;
		this.body = body;
		this.createdAt = createdAt;
	}

}
