package com.example.school.view;

import java.util.List;

import com.example.school.model.Student;

public interface StudentView {

	void showAllStudents(List<Student> students);

	void studentAdded(Student student);

	void showError(String message, Student student);

	void studentRemoved(Student student);

}
