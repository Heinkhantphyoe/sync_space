package com.hkp.sync_space.board;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.hkp.sync_space.space.Space;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "board_columns")
@Getter
@Setter
@NoArgsConstructor
public class BoardColumn {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false)
	private Space space;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(nullable = false)
	private int position;

	public BoardColumn(Space space, String name, int position) {
		this.id = UUID.randomUUID();
		this.space = space;
		this.name = name;
		this.position = position;
	}

}
