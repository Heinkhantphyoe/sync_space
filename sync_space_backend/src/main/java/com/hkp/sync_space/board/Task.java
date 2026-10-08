package com.hkp.sync_space.board;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.hkp.sync_space.space.Space;
import com.hkp.sync_space.user.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false)
	private Space space;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "column_id", nullable = false)
	private BoardColumn column;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 4000)
	private String description;

	@Column(nullable = false)
	private int position;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@ManyToMany
	@JoinTable(name = "task_assignees", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
	private Set<User> assignees = new LinkedHashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskPriority priority;

	@ElementCollection
	@CollectionTable(name = "task_labels", joinColumns = @JoinColumn(name = "task_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "label", nullable = false, length = 20)
	private Set<TaskLabel> labels = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
	private Instant updatedAt;

	public Task(Space space, BoardColumn column, String title, String description, int position, User createdBy) {
		this.id = UUID.randomUUID();
		this.space = space;
		this.column = column;
		this.title = title;
		this.description = description;
		this.position = position;
		this.createdBy = createdBy;
		this.assignees = new LinkedHashSet<>();
		this.priority = TaskPriority.MEDIUM;
		this.labels = new LinkedHashSet<>();
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

}
