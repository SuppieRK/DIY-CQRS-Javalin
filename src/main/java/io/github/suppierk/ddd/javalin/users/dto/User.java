package io.github.suppierk.ddd.javalin.users.dto;

import io.github.suppierk.example.tables.records.UsersRecord;
import java.util.UUID;

/**
 * Publicly exposed Data Transfer Object which provides more serialization options than jOOQ {@link
 * UsersRecord}.
 *
 * @param username of the user
 * @param email of the user
 */
public record User(UUID id, String username, String email) {
  /** Alternative constructor, which fetches some of the {@link UsersRecord} options. */
  public User(UsersRecord usersRecord) {
    this(usersRecord.getId(), usersRecord.getUsername(), usersRecord.getEmail());
  }
}
