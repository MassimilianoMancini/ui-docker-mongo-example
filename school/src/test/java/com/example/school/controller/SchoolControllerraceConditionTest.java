package com.example.school.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.school.model.Student;
import com.example.school.repository.StudentRepository;
import com.example.school.view.StudentView;

@ExtendWith(MockitoExtension.class)
public class SchoolControllerraceConditionTest {

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private StudentView studentView;

	@InjectMocks
	private SchoolController schoolController;

	@Test
	void testNewStudentConcurrent() {
		List<Student> students = new ArrayList<>();
		Student student = new Student("1", "name");
		// stub the StudentRepository
		when(studentRepository.findById(anyString()))
				.thenAnswer(invocation -> students.stream().findFirst().orElse(null));
		doAnswer(invocation -> {
			students.add(student);
			return null;
		}).when(studentRepository).save(any(Student.class));
		// start the threads calling newStudent concurrently
		List<Thread> threads = IntStream.range(0, 10)
				.mapToObj(i -> new Thread(() -> schoolController.newStudent(student))).peek(t -> t.start())
				.collect(Collectors.toList());
		await().atMost(10, TimeUnit.SECONDS).until(() -> threads.stream().noneMatch(t -> t.isAlive()));
		assertThat(students).containsExactly(student);

	}

}
