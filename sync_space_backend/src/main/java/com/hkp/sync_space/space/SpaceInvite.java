package com.hkp.sync_space.space;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.hkp.sync_space.user.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "space_invites", uniqueConstraints = @UniqueConstraint(columnNames = { "space_id", "email" }))
@Getter
@Setter
@NoArgsConstructor
public class SpaceInvite {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "space_id", nullable = false)
	private Space space;

	@Column(nullable = false, length = 255)
	private String email;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "invited_by", nullable = false)
	private User invitedBy;

	@Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
	private Instant createdAt;

	public SpaceInvite(Space space, String email, User invitedBy) {
		this.id = UUID.randomUUID();
		this.space = space;
		this.email = email;
		this.invitedBy = invitedBy;
		this.createdAt = Instant.now();
	}

}
