package com.ekote;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ekote.controller.HomeController;
import org.junit.jupiter.api.Test;

class DemoApplicationTests {

  private final HomeController homeController = new HomeController();

  @Test
  void homePageShouldReturnIndexView() {
    assertEquals("index", homeController.home());
  }

  @Test
  void loginPageShouldReturnLoginView() {
    assertEquals("login", homeController.login());
  }

  @Test
  void dashboardShouldReturnDashboardView() {
    assertEquals("dashboard", homeController.dashboard());
  }

  @Test
  void registerPageShouldReturnRegisterView() {
    assertEquals("register", homeController.register());
  }
}
