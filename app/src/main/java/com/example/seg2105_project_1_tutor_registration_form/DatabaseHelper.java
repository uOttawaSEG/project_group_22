package com.example.seg2105_project_1_tutor_registration_form;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.example.seg2105_project_1_tutor_registration_form.model.Tutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "TutorApp.db";
    // v1: tutors; v2: admins; v3: students
    private static final int DATABASE_VERSION = 3;

    // ---------- Tutors table ----------
    private static final String TABLE_TUTORS      = "tutors";
    private static final String COLUMN_ID         = "id";
    private static final String COLUMN_FIRST_NAME = "first_name";
    private static final String COLUMN_LAST_NAME  = "last_name";
    private static final String COLUMN_EMAIL      = "email";
    private static final String COLUMN_PASSWORD   = "password";
    private static final String COLUMN_PHONE      = "phone";
    private static final String COLUMN_DEGREE     = "degree";
    private static final String COLUMN_COURSES    = "courses"; // comma-separated

    // ---------- Admins table ----------
    private static final String TABLE_ADMINS = "Admins";
    private static final String COL_A_ID     = "id";
    private static final String COL_A_NAME   = "name";
    private static final String COL_A_EMAIL  = "email";
    private static final String COL_A_SALT   = "salt";
    private static final String COL_A_HASH   = "password_hash";

    // ---------- Students table ----------
    private static final String TABLE_STUDENTS        = "students";
    private static final String COL_S_ID              = "id";
    private static final String COL_S_FIRST_NAME      = "first_name";
    private static final String COL_S_LAST_NAME       = "last_name";
    private static final String COL_S_EMAIL           = "email";
    private static final String COL_S_PASSWORD        = "password";
    private static final String COL_S_PHONE           = "phone";          // optional
    private static final String COL_S_STUDENT_ID      = "student_id";     // uOttawa ID
    private static final String COL_S_PROGRAM         = "program";        // Program/Major
    private static final String COL_S_STUDY_YEAR      = "study_year";     // "1st year"... "Graduate"
    private static final String COL_S_COURSES_WANTED  = "courses_wanted"; // comma-separated
    private static final String COL_S_NOTES           = "notes";          // optional

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        // Tutors
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TUTORS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_FIRST_NAME + " TEXT, "
                + COLUMN_LAST_NAME + " TEXT, "
                + COLUMN_EMAIL + " TEXT UNIQUE, "
                + COLUMN_PASSWORD + " TEXT, "
                + COLUMN_PHONE + " TEXT, "
                + COLUMN_DEGREE + " TEXT, "
                + COLUMN_COURSES + " TEXT"
                + ")");

        // Admins
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ADMINS + " ("
                + COL_A_ID   + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_A_NAME + " TEXT, "
                + COL_A_EMAIL+ " TEXT UNIQUE NOT NULL, "
                + COL_A_SALT + " TEXT NOT NULL, "
                + COL_A_HASH + " TEXT NOT NULL"
                + ")");

        // Students
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STUDENTS + " ("
                + COL_S_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_S_FIRST_NAME     + " TEXT, "
                + COL_S_LAST_NAME      + " TEXT, "
                + COL_S_EMAIL          + " TEXT UNIQUE, "
                + COL_S_PASSWORD       + " TEXT, "
                + COL_S_PHONE          + " TEXT, "
                + COL_S_STUDENT_ID     + " TEXT, "
                + COL_S_PROGRAM        + " TEXT, "
                + COL_S_STUDY_YEAR     + " TEXT, "
                + COL_S_COURSES_WANTED + " TEXT, "
                + COL_S_NOTES          + " TEXT"
                + ")");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ADMINS + " ("
                    + COL_A_ID   + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_A_NAME + " TEXT, "
                    + COL_A_EMAIL+ " TEXT UNIQUE NOT NULL, "
                    + COL_A_SALT + " TEXT NOT NULL, "
                    + COL_A_HASH + " TEXT NOT NULL"
                    + ")");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STUDENTS + " ("
                    + COL_S_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_S_FIRST_NAME     + " TEXT, "
                    + COL_S_LAST_NAME      + " TEXT, "
                    + COL_S_EMAIL          + " TEXT UNIQUE, "
                    + COL_S_PASSWORD       + " TEXT, "
                    + COL_S_PHONE          + " TEXT, "
                    + COL_S_STUDENT_ID     + " TEXT, "
                    + COL_S_PROGRAM        + " TEXT, "
                    + COL_S_STUDY_YEAR     + " TEXT, "
                    + COL_S_COURSES_WANTED + " TEXT, "
                    + COL_S_NOTES          + " TEXT"
                    + ")");
        }
    }

    // ===================== TUTORS =====================

    public boolean addTutor(Tutor tutor) {
        if (tutor == null || TextUtils.isEmpty(tutor.getEmail())) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FIRST_NAME, tutor.getFirstName());
        values.put(COLUMN_LAST_NAME,  tutor.getLastName());
        values.put(COLUMN_EMAIL,      tutor.getEmail());
        values.put(COLUMN_PASSWORD,   tutor.getPassword());
        values.put(COLUMN_PHONE,      tutor.getPhone());
        values.put(COLUMN_DEGREE,     tutor.getDegree());
        List<String> courses = tutor.getCourses() != null ? tutor.getCourses() : new ArrayList<>();
        values.put(COLUMN_COURSES, TextUtils.join(",", courses));
        long result = db.insert(TABLE_TUTORS, null, values);
        return result != -1;
    }

    public boolean updateTutorPassword(String email, String newPassword) {
        SQLiteDatabase dbw = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PASSWORD, newPassword);
        int rows = dbw.update(TABLE_TUTORS, cv, COLUMN_EMAIL + " = ?", new String[]{ email });
        return rows > 0;
    }

    public Tutor getTutor(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {
                COLUMN_ID, COLUMN_FIRST_NAME, COLUMN_LAST_NAME, COLUMN_EMAIL,
                COLUMN_PHONE, COLUMN_DEGREE, COLUMN_COURSES
        };
        Tutor tutor = null;
        try (Cursor cursor = db.query(
                TABLE_TUTORS, columns,
                COLUMN_EMAIL + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{email, password}, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                String id         = String.valueOf(cursor.getLong(0));
                String firstName  = cursor.getString(1);
                String lastName   = cursor.getString(2);
                String em         = cursor.getString(3);
                String phone      = cursor.getString(4);
                String degree     = cursor.getString(5);
                String coursesStr = cursor.getString(6);

                List<String> courses = new ArrayList<>();
                if (!TextUtils.isEmpty(coursesStr)) {
                    courses = new ArrayList<>(Arrays.asList(coursesStr.split(",")));
                    for (int i = 0; i < courses.size(); i++) {
                        courses.set(i, courses.get(i).trim());
                    }
                }
                tutor = new com.example.seg2105_project_1_tutor_registration_form.model.Tutor(
                        id, firstName, lastName, em, password, phone, degree, courses
                );
            }
        }
        return tutor;
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_TUTORS, new String[]{COLUMN_EMAIL},
                COLUMN_EMAIL + "=?", new String[]{email}, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // ===================== ADMINS =====================

    /** true if an Admin with this email exists */
    public boolean adminEmailExists(String email) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_ADMINS + " WHERE " + COL_A_EMAIL + " = ? LIMIT 1",
                new String[]{ email })) {
            return c.moveToFirst();
        }
    }

    /** insert a new Admin (salt+hash already computed). returns true on success */
    public boolean insertAdmin(String name, String email, String salt, String passwordHash) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_A_NAME,  name);
        values.put(COL_A_EMAIL, email);
        values.put(COL_A_SALT,  salt);
        values.put(COL_A_HASH,  passwordHash);

        long rowId = db.insertWithOnConflict(
                TABLE_ADMINS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return rowId != -1;
    }

    /** verify admin login using salt+hash */
    public boolean verifyAdminLogin(String email, String rawPassword) {
        SQLiteDatabase db = getReadableDatabase();
        String salt = null, hash = null;
        try (Cursor c = db.rawQuery(
                "SELECT " + COL_A_SALT + ", " + COL_A_HASH +
                        " FROM " + TABLE_ADMINS +
                        " WHERE " + COL_A_EMAIL + " = ? LIMIT 1",
                new String[]{ email })) {
            if (c.moveToFirst()) {
                salt = c.getString(0);
                hash = c.getString(1);
            }
        }
        if (salt == null || hash == null) return false;

        String recomputed = com.example.seg2105_project_1_tutor_registration_form.auth.SecurityUtils
                .sha256(rawPassword + salt);
        return hash.equals(recomputed);
    }

    // ===================== STUDENTS =====================

    public boolean addStudent(String firstName, String lastName, String email,
                              String password, String phone, String studentId,
                              String program, String studyYear,
                              List<String> coursesWanted, String notes) {

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return false;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_S_FIRST_NAME, firstName);
        v.put(COL_S_LAST_NAME,  lastName);
        v.put(COL_S_EMAIL,      email);
        v.put(COL_S_PASSWORD,   password);
        v.put(COL_S_PHONE,      phone);
        v.put(COL_S_STUDENT_ID, studentId);
        v.put(COL_S_PROGRAM,    program);
        v.put(COL_S_STUDY_YEAR, studyYear);
        v.put(COL_S_COURSES_WANTED,
                coursesWanted == null ? "" : TextUtils.join(",", coursesWanted));
        v.put(COL_S_NOTES,      notes);

        long rowId = db.insertWithOnConflict(
                TABLE_STUDENTS, null, v, SQLiteDatabase.CONFLICT_ABORT);
        return rowId != -1;
    }

    public boolean studentEmailExists(String email) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_STUDENTS +
                        " WHERE " + COL_S_EMAIL + "=? LIMIT 1",
                new String[]{ email })) {
            return c.moveToFirst();
        }
    }

    // simple email+password check (switch to salt+hash if you prefer)
    public boolean verifyStudentLogin(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM " + TABLE_STUDENTS +
                        " WHERE " + COL_S_EMAIL + "=? AND " + COL_S_PASSWORD + "=? LIMIT 1",
                new String[]{ email, password })) {
            return c.moveToFirst();
        }
    }

    public boolean updateStudentPassword(String email, String newPassword) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_S_PASSWORD, newPassword);
        int rows = db.update(TABLE_STUDENTS, cv, COL_S_EMAIL + "=?", new String[]{ email });
        return rows > 0;
    }
}
