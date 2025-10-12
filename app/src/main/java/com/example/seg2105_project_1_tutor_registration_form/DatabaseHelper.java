package com.example.seg2105_project_1_tutor_registration_form;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "TutorApp.db";
    private static final int DATABASE_VERSION = 1;

    // Tutors table
    private static final String TABLE_TUTORS = "tutors";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FIRST_NAME = "first_name";
    private static final String COLUMN_LAST_NAME = "last_name";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_DEGREE = "degree";
    private static final String COLUMN_COURSES = "courses";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TUTORS_TABLE = "CREATE TABLE " + TABLE_TUTORS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_FIRST_NAME + " TEXT,"
                + COLUMN_LAST_NAME + " TEXT,"
                + COLUMN_EMAIL + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_PHONE + " TEXT,"
                + COLUMN_DEGREE + " TEXT,"
                + COLUMN_COURSES + " TEXT" + ")";
        db.execSQL(CREATE_TUTORS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TUTORS);
        onCreate(db);
    }

    public boolean addTutor(Tutor tutor) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FIRST_NAME, tutor.getFirstName());
        values.put(COLUMN_LAST_NAME, tutor.getLastName());
        values.put(COLUMN_EMAIL, tutor.getEmail());
        values.put(COLUMN_PASSWORD, tutor.getPassword());
        values.put(COLUMN_PHONE, tutor.getPhone());
        values.put(COLUMN_DEGREE, tutor.getDegree());
        values.put(COLUMN_COURSES, String.join(",", tutor.getCourses()));

        long result = db.insert(TABLE_TUTORS, null, values);
        return result != -1;
    }

    public Tutor getTutor(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_FIRST_NAME, COLUMN_LAST_NAME, COLUMN_EMAIL,
                COLUMN_PHONE, COLUMN_DEGREE, COLUMN_COURSES};
        String selection = COLUMN_EMAIL + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};

        Cursor cursor = db.query(TABLE_TUTORS, columns, selection, selectionArgs,
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Tutor tutor = new Tutor();
            tutor.setFirstName(cursor.getString(0));
            tutor.setLastName(cursor.getString(1));
            tutor.setEmail(cursor.getString(2));
            tutor.setPhone(cursor.getString(3));
            tutor.setDegree(cursor.getString(4));

            String coursesStr = cursor.getString(5);
            List<String> courses = new ArrayList<>();
            if (coursesStr != null) {
                String[] courseArray = coursesStr.split(",");
                for (String course : courseArray) {
                    courses.add(course.trim());
                }
            }
            tutor.setCourses(courses);

            cursor.close();
            return tutor;
        }
        return null;
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_EMAIL};
        String selection = COLUMN_EMAIL + " = ?";
        String[] selectionArgs = {email};

        Cursor cursor = db.query(TABLE_TUTORS, columns, selection, selectionArgs,
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}