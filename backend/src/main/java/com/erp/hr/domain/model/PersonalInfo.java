package com.erp.hr.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

@Embeddable
public class PersonalInfo {

  @Column(name = "last_name", nullable = false, length = 50)
  private String lastName;

  @Column(name = "first_name", nullable = false, length = 50)
  private String firstName;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", length = 10)
  private Gender gender;

  @Column(name = "national_id", length = 50)
  private String nationalId;

  @Column(name = "phone", length = 30)
  private String phone;

  @Column(name = "personal_email", length = 200)
  private String personalEmail;

  protected PersonalInfo() {}

  public PersonalInfo(
      String lastName,
      String firstName,
      LocalDate dateOfBirth,
      Gender gender,
      String nationalId,
      String phone,
      String personalEmail) {
    this.lastName = lastName;
    this.firstName = firstName;
    this.dateOfBirth = dateOfBirth;
    this.gender = gender;
    this.nationalId = nationalId;
    this.phone = phone;
    this.personalEmail = personalEmail;
  }

  public String getFullName() {
    return lastName + " " + firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public Gender getGender() {
    return gender;
  }

  public String getNationalId() {
    return nationalId;
  }

  public String getPhone() {
    return phone;
  }

  public String getPersonalEmail() {
    return personalEmail;
  }

  public enum Gender {
    MALE,
    FEMALE,
    OTHER
  }
}
