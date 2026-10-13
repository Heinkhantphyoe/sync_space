package com.hkp.sync_space.board;

import java.time.LocalDate;

public record SetTaskDueRequest(LocalDate dueOn) {
}
