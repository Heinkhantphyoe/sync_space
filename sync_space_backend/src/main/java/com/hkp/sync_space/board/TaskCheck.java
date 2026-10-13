package com.hkp.sync_space.board;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_checks")
@Getter
@Setter
@NoArgsConstructor
public class TaskCheck {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private boolean done;

	@Column(nullable = false)
	private int position;

	public TaskCheck(Task task, String title, int position) {
		this.id = UUID.randomUUID();
		this.task = task;
		this.title = title;
		this.done = false;
		this.position = position;
	}

}
