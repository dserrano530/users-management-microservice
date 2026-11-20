package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.exception.InvalidCredentialsException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.io.ConsoleIO;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.controller.UserController;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.CreateUserRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.LoginRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.UpdateUserRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.UserResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class UserManagementCliTest {

  private static final String EMAIL = "member@example.com";
  private static final UserResponse USER = new UserResponse("u-1", "Test Member", EMAIL, "MEMBER", "ACTIVE");
  private final UserController controller = mock(UserController.class);

  @Test
  void readsOperationsAndPrintsTheirResults() {
    when(controller.listAllUsers()).thenReturn(List.of(USER));
    when(controller.findUserById("u-1")).thenReturn(USER);
    when(controller.createUser(any())).thenReturn(USER);
    when(controller.updateUser(any())).thenReturn(USER);
    when(controller.login(any())).thenReturn(USER);

    final String output = run(
        "oops", "99",
        "1", "2", "", " u-1 ",
        "3", "u-1", "Test Member", EMAIL, "Example123!", "MEMBER",
        "4", "u-1", "Test Member", EMAIL, "", "MEMBER", "ACTIVE",
        "5", "u-1",
        "6", EMAIL, "Example123!",
        "0");

    assertThat(output).contains(
        "Invalid input", "Invalid option", "Value cannot be blank", "Total: 1 user(s)",
        "User created successfully", "User updated successfully", "User deleted successfully",
        "Login successful", "Goodbye!", EMAIL).doesNotContain("Example123!");
    verify(controller).findUserById("u-1");
    verify(controller).createUser(new CreateUserRequest("u-1", "Test Member", EMAIL, "Example123!", "MEMBER"));
    verify(controller).updateUser(new UpdateUserRequest("u-1", "Test Member", EMAIL, null, "MEMBER", "ACTIVE"));
    verify(controller).deleteUser("u-1");
    verify(controller).login(new LoginRequest(EMAIL, "Example123!"));
  }

  @Test
  void recoversFromOperationErrorsAndReturnsToTheMenu() {
    final UserNotFoundException missing = UserNotFoundException.becauseIdWasNotFound("missing");
    when(controller.listAllUsers()).thenReturn(List.of()).thenThrow(new IllegalStateException("offline"));
    when(controller.findUserById("missing")).thenThrow(missing);
    when(controller.createUser(any())).thenThrow(UserAlreadyExistsException.becauseEmailAlreadyExists(EMAIL));
    when(controller.updateUser(any())).thenThrow(missing);
    doThrow(missing).when(controller).deleteUser("missing");
    when(controller.login(any())).thenThrow(InvalidCredentialsException.becauseCredentialsAreInvalid());

    final String output = run(
        "1", "1", "2", "missing",
        "3", "u-1", "Test Member", EMAIL, "Example123!", "MEMBER",
        "4", "missing", "Test Member", EMAIL, "Changed123!", "MEMBER", "ACTIVE",
        "5", "missing",
        "6", EMAIL, "Incorrect123!",
        "0");

    assertThat(output).contains(
        "No users found", "Unexpected error: offline", "Not found:", "already exists",
        "Correo o contraseña incorrectos", "Goodbye!").doesNotContain("Login successful");
  }

  private String run(final String... input) {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (Scanner scanner = new Scanner(String.join("\n", input));
         PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8)) {
      new UserManagementCli(controller, new ConsoleIO(scanner, out)).start();
    }
    return output.toString(StandardCharsets.UTF_8);
  }
}
