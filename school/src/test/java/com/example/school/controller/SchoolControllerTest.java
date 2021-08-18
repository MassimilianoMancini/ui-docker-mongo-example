package com.example.school.controller;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.school.model.Student;
import com.example.school.repository.StudentRepository;
import com.example.school.view.StudentView;

@ExtendWith(MockitoExtension.class)
class SchoolControllerTest {

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private StudentView studentView;

	@InjectMocks
	private SchoolController schoolController;

	@Test
	void testAllStudents() {
		List<Student> students = asList(new Student());
		when(studentRepository.findAll()).thenReturn(students);
		schoolController.allStudents();
		verify(studentView).showAllStudents(students);
	}

	@Test
	void testNewStudentWhenStudentDoesNotAlreadyExist() {
		Student student = new Student("1", "test");
		when(studentRepository.findById("1")).thenReturn(null);
		schoolController.newStudent(student);
		InOrder inOrder = Mockito.inOrder(studentRepository, studentView);
		inOrder.verify(studentRepository).save(student);
		inOrder.verify(studentView).studentAdded(student);
		verifyNoMoreInteractions(ignoreStubs(studentRepository));
	}

	@Test
	void testNewStudentWhenStudentAlreadyExists() {
		Student studentToAdd = new Student("1", "test");
		Student existingStudent = new Student("1", "name");
		when(studentRepository.findById("1")).thenReturn(existingStudent);
		schoolController.newStudent(studentToAdd);
		verify(studentView).showError("Already existing student with id 1", existingStudent);
		verifyNoMoreInteractions(ignoreStubs(studentRepository));
	}

	@Test
	void testDeleteStudentWhenStudentExists() {
		Student studentToDelete = new Student("1", "test");
		when(studentRepository.findById("1")).thenReturn(studentToDelete);
		schoolController.deleteStudent(studentToDelete);
		InOrder inOrder = Mockito.inOrder(studentRepository, studentView);
		inOrder.verify(studentRepository).delete("1");
		inOrder.verify(studentView).studentRemoved(studentToDelete);
		verifyNoMoreInteractions(ignoreStubs(studentRepository));
	}

	@Test
	void testDeleteStudentWhenStudentDoesNotExist() {
		Student studentToDelete = new Student("1", "test");
		when(studentRepository.findById("1")).thenReturn(null);
		schoolController.deleteStudent(studentToDelete);
		verify(studentView).showError("No existing student with id 1", studentToDelete);
		verifyNoMoreInteractions(ignoreStubs(studentRepository));
	}
}
