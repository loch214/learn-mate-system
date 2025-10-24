# 🚀 How to Run the Application

## If you see this error
```
Error: Could not find or load main class com.learnmate.LearnMateApplication
Caused by: java.lang.ClassNotFoundException: com.learnmate.LearnMateApplication
```
This happens when running with plain `java` without the application’s dependencies on the classpath, or when using a non-executable jar. Use one of the methods below.

## ✅ Recommended: Run from IDE

### Option 1: IntelliJ IDEA
1. **Open the project** in IntelliJ IDEA
2. **Wait for Maven import** to complete (bottom status bar)
3. **Find the main class**: `src/main/java/com/learnmate/LearnMateApplication.java`
4. **Right-click** on `LearnMateApplication.java`
5. **Select "Run 'LearnMateApplication.main()'"**
6. **Or click the green play button** next to the main method

### Option 2: VS Code (if you have Java extensions)
1. **Open the project** in VS Code
2. **Install Java Extension Pack** if not already installed
3. **Press F5** or use Command Palette → "Java: Run"
4. **Select the main class** when prompted

### Option 3: Eclipse
1. **Import the project** as Maven project
2. **Right-click** on the project → **Run As** → **Java Application**
3. **Select** `LearnMateApplication` as the main class

## ⚡ **Quick Test After Starting**

Once the application starts successfully:

1. **Open browser**: `http://localhost:8080`
2. **Login as a STUDENT**
3. **Go to Exams page**: `http://localhost:8080/exams/list`

### ✅ **You Should Now See:**
- ❌ **No "Create New Exam" button** (only for teachers)
- ❌ **No Edit/Delete buttons** (only for teachers)
- ✅ **"View Only" text** instead of action buttons
- ✅ **Only student-appropriate navigation menu**

### 🔧 **Navigation Should Show (for STUDENT):**
- Timetables ✅
- Attendances ✅  
- Exams ✅
- Marks ✅
- Notifications ✅
- Profile ✅

### ❌ **Navigation Should NOT Show (for STUDENT):**
- Users (Admin only)
- Subjects (Admin only)
- Classes (Admin only)
- Reports (Admin only)
- Fees (Parent/Admin only)

## 🐛 **If IDE Shows Errors:**

### Common Issues:
1. **"Cannot resolve Spring Boot"** → Reload Maven project
2. **"JDK not found"** → Set Project SDK to Java 17 or higher
3. **"Dependencies not found"** → Maven clean + reload

### Quick Fixes:
- **IntelliJ**: File → Reload Gradle/Maven Project
- **VS Code**: Command Palette → Java: Reload Projects
- **Eclipse**: Right-click project → Maven → Reload

## 📋 **Alternative: Install Maven (Optional)**

If you want to use command line:

1. **Download Maven**: https://maven.apache.org/download.cgi
2. **Extract** to a folder (e.g., `C:\apache-maven-3.9.6`)
3. **Add to PATH**: Add `C:\apache-maven-3.9.6\bin` to system PATH
4. **Restart terminal**
5. **Run**: `mvn clean compile spring-boot:run`

---

**🎯 The role-based security fixes are ready - just need to start the app through your IDE!**