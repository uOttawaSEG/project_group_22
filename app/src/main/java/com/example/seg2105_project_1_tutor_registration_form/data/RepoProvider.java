package com.example.seg2105_project_1_tutor_registration_form.data;

import com.example.seg2105_project_1_tutor_registration_form.data.tutor.FirestoreTutorRepository;
import com.example.seg2105_project_1_tutor_registration_form.data.tutor.TutorRepository;

public final class RepoProvider {
    private static final TutorRepository TUTOR = new FirestoreTutorRepository(); // swap to real when implemented
    private RepoProvider() {}
    public static TutorRepository tutor() { return TUTOR; }
}
