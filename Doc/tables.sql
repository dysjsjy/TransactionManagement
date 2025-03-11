-- 测试用数据库表 --

DROP DATABASE IF EXISTS dyTM;
CREATE DATABASE dyTM;

USE dyTM;
-- ... existing code ...
-- Create the Teachers table
CREATE TABLE Teachers
(
    teacher_id INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(100),
    phone      VARCHAR(15)
);

-- Create the Classes table
CREATE TABLE Classes
(
    class_id   INT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(100) NOT NULL,
    teacher_id INT,
    FOREIGN KEY (teacher_id) REFERENCES Teachers (teacher_id)
);

-- Create the Students table
CREATE TABLE Students
(
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    class_id   INT,
    email      VARCHAR(100),
    phone      VARCHAR(15),
    FOREIGN KEY (class_id) REFERENCES Classes (class_id)
);

-- Create the Courses table
CREATE TABLE Courses
(
    course_id   INT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    teacher_id  INT,
    class_id    INT,
    FOREIGN KEY (teacher_id) REFERENCES Teachers (teacher_id),
    -- Note: You need to add a closing parenthesis here
    FOREIGN KEY (class_id) REFERENCES Classes (class_id)
);

-- Create the StudentCourses table
CREATE TABLE StudentCourses
(
    student_id INT,
    course_id  INT,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Students (student_id),
    FOREIGN KEY (course_id) REFERENCES Courses (course_id)
);
-- ... existing code ...