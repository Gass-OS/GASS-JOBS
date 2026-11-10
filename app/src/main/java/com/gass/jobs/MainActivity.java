package com.gass.jobs;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(245, 246, 250);
    private final int muted = Color.rgb(171, 178, 190);
    private final int green = Color.rgb(245, 178, 35);
    private final int canvas = Color.rgb(10, 11, 14);
    private final int panel = Color.rgb(25, 27, 33);
    private final int gold = Color.rgb(255, 190, 45);
    private LinearLayout root;
    private LinearLayout content;
    private boolean employerMode = false;
    private EditText searchInput;
    private Spinner governorateFilter;
    private Spinner sectorFilter;
    private Spinner contractFilter;
    private LinearLayout resultsContainer;
    private final ArrayList<Runnable> screenHistory = new ArrayList<Runnable>();
    private Runnable currentScreenAction;
    private Runnable pendingScreenAction;
    private boolean pendingRootScreen;
    private boolean restoringScreen;
    private static final int PICK_PROFILE_PHOTO = 7101;
    private static final int PICK_PROFILE_CV = 7102;
    private ImageView profilePhotoView;
    private TextView profileCvLabel;
    private Location userLocation;
    private boolean sortByDistance = false;
    private TextView locationStatus;
    private static final int REQUEST_LOCATION_PERMISSION = 7201;
    private int swipeIndex = 0;
    private int candidateSwipeIndex = 0;
    private final ArrayList<Job> visibleJobs = new ArrayList<Job>();

    private static final int TAB_HOME = 0, TAB_OFFERS = 1, TAB_PROFILE = 2, TAB_EMPLOYERS = 3, TAB_MORE = 4;
    private FrameLayout deckHolder;
    private LinearLayout actionBar;
    private TextView likeStamp, nopeStamp;
    private boolean animating = false;
    private boolean profileVisible = false;
    private boolean preferSignupOnAccount = false;
    private final ExecutorService authExecutor = Executors.newSingleThreadExecutor();

    private static class Job {
        final String title, company, location, governorate, sector, contract, description;
        final String sourceUrl, publishedDate;
        Job(String title, String company, String location, String governorate, String sector, String contract, String description) {
            this(title, company, location, governorate, sector, contract, description, "", "");
        }
        Job(String title, String company, String location, String governorate, String sector, String contract,
            String description, String sourceUrl, String publishedDate) {
            this.title = title;
            this.company = company;
            this.location = location;
            this.governorate = governorate;
            this.sector = sector;
            this.contract = contract;
            this.description = description;
            this.sourceUrl = sourceUrl;
            this.publishedDate = publishedDate;
        }
        boolean isOfficial() { return sourceUrl != null && !sourceUrl.isEmpty(); }
        String key() { return title + "|" + company + "|" + location; }
    }

    private static class Candidate {
        final String name, profession, city, experience, education, skills, availability;
        Candidate(String name, String profession, String city, String experience, String education, String skills, String availability) {
            this.name = name;
            this.profession = profession;
            this.city = city;
            this.experience = experience;
            this.education = education;
            this.skills = skills;
            this.availability = availability;
        }
        String key() { return name + "|" + profession + "|" + city; }
    }

    // Fictional examples used only to demonstrate the candidate discovery UI.
    private final List<Candidate> demoCandidates = new ArrayList<Candidate>() {{
        add(new Candidate("Sarra Ben Ali", "Assistante administrative", "Kasserine", "2 ans", "BTS gestion", "Bureautique • Accueil • Organisation", "Disponible immédiatement"));
        add(new Candidate("Youssef Trabelsi", "Technicien informatique", "Sbeitla", "3 ans", "Technicien supérieur", "Maintenance • Réseaux • Support", "Disponible sous 2 semaines"));
        add(new Candidate("Meriem Gharbi", "Comptable", "Kasserine", "4 ans", "Licence comptabilité", "Sage • Excel • Facturation", "Temps plein"));
        add(new Candidate("Fares Jlassi", "Électricien bâtiment", "Fériana", "5 ans", "CAP électricité", "Installation • Sécurité • Maintenance", "Disponible immédiatement"));
        add(new Candidate("Ines Khemiri", "Vendeuse / Conseillère", "Kairouan", "1 an", "Baccalauréat", "Vente • Relation client • Caisse", "Temps plein ou partiel"));
    }};

    // Fictional examples used only to demonstrate the search UI.
    private final List<Job> demoJobs = new ArrayList<Job>() {{
        add(new Job("Agent commercial", "Entreprise de démonstration", "Kasserine", "Kasserine", "Commerce", "Temps plein",
                "Relation client, prospection et suivi commercial. Expérience souhaitée."));
        add(new Job("Technicien informatique", "Entreprise de démonstration", "Sbeitla", "Kasserine", "Informatique", "CDI",
                "Assistance informatique, maintenance et support aux utilisateurs."));
        add(new Job("Ouvrier agricole", "Exploitation de démonstration", "Fériana", "Kasserine", "Agriculture", "Saisonnier",
                "Travaux agricoles. Débutants acceptés, formation sur place."));
        add(new Job("Électricien bâtiment", "Société fictive", "Kairouan", "Kairouan", "Bâtiment", "CDD",
                "Installation électrique et maintenance sur chantiers. Exemple fictif."));
        add(new Job("Aide-soignant", "Centre de démonstration", "Sfax", "Sfax", "Santé", "Temps plein",
                "Accompagnement des patients et aide aux soins. Exemple fictif."));
        add(new Job("Chauffeur-livreur", "Entreprise fictive", "Tunis", "Tunis", "Transport", "CDI",
                "Livraison locale et suivi des commandes. Exemple fictif."));
    }};

    private static final String MINISTRY_DIRECTORY =
            "https://www.emploi.gov.tn/fr/93/directions-regionales-de-formation-professionnelle-et-de-lemploi";
    private static final String OPEN_DATA_DIRECTORY =
            "https://catalog.data.gov.tn/fr/dataset/reseau-de-bureaux-de-l-emploi-et-du-travail-independant-en-tunisie";
    private static final String ANETI_TUNISIA_OFFERS =
            "https://www.emploi.nat.tn/fo/Fr2026/global.php?menu1=171";
    private static final String ANETI_INTERNATIONAL_OFFERS =
            "https://www.aneti-international.tn/index.php/offres";

    // Real vacancies transcribed from ANETI International's public listing on 10 Nov 2026.
    // Each card opens the original ANETI vacancy page. No end date was displayed on the source pages.
    private final List<Job> anetiJobs = new ArrayList<Job>() {{
        add(new Job("Mécanicien d'équipement agricole", "ANETI — Emploi international", "Canada", "International",
                "Mécanique", "Permanent • Temps plein",
                "Publié le 03/11/2026. Diagnostic, entretien et réparation de machines agricoles. Profil publié par ANETI : au moins 3 ans d'expérience en mécanique. Salaire annoncé : 26 à 30 CAD/heure selon expérience. CV exigé en anglais. Vérifiez toutes les conditions sur la fiche officielle.",
                "https://www.aneti-international.tn/node/1071902", "03/11/2026"));
        add(new Job("Cuisinier", "ANETI — Emploi international", "France", "International",
                "Hôtellerie • Restauration", "Saisonnier",
                "Publié le 03/11/2026. Préparation des repas, organisation du travail, hygiène et sécurité alimentaire, gestion des matières premières et travail en équipe. Formation professionnelle en cuisine ou diplôme équivalent souhaité. Consultez la fiche ANETI pour les conditions complètes.",
                "https://www.aneti-international.tn/node/1071991", "03/11/2026"));
        add(new Job("Lingère", "ANETI — Emploi international", "France", "International",
                "Hôtellerie • Restauration", "Saisonnier",
                "Publié le 22/10/2026. Lavage, séchage, repassage et réparation du linge. La fiche ANETI demande rigueur, organisation, esprit d'équipe et au moins 2 ans d'expérience dans l'hôtellerie, la restauration ou un secteur similaire. CV en français.",
                "https://www.aneti-international.tn/node/1064795", "22/10/2026"));
        add(new Job("Femme de ménage", "ANETI — Emploi international", "France", "International",
                "Hôtellerie • Services", "À vérifier sur l'annonce",
                "Publié le 22/10/2026. Offre publiée dans la liste officielle ANETI International. Ouvrez la fiche source pour consulter les missions, le profil recherché et les modalités de candidature.",
                "https://www.aneti-international.tn/node/1064776", "22/10/2026"));
        add(new Job("Chaudronnier aéronautique expérimenté", "ANETI — Emploi international", "France", "International",
                "Mécanique • Industrie", "CDI",
                "Publié le 26/10/2026. Offre affichée dans la liste publique ANETI International. Consultez l'annonce originale pour le profil, les compétences et les modalités.",
                "https://www.aneti-international.tn/node/1067610", "26/10/2026"));
        add(new Job("Rectifieurs cylindriques", "ANETI — Emploi international", "France", "International",
                "Mécanique • Industrie", "CDI",
                "Publié le 26/10/2026. Offre affichée dans la liste publique ANETI International. Consultez l'annonce originale pour le profil, les compétences et les modalités.",
                "https://www.aneti-international.tn/node/1067617", "26/10/2026"));
        add(new Job("Régleurs tournage multi-broches CN", "ANETI — Emploi international", "France", "International",
                "Mécanique • Maintenance", "CDI",
                "Publié le 26/10/2026. Réglage de machines de tournage CN, lecture de plans et contrôle dimensionnel. La fiche source indique 5 ans minimum d'expérience en usinage de précision, horaires d'équipe et français requis.",
                "https://www.aneti-international.tn/node/1067623", "26/10/2026"));
        add(new Job("Techniciens usinage fraisage CN 5 axes", "ANETI — Emploi international", "France", "International",
                "Mécanique • Industrie", "À vérifier sur l'annonce",
                "Publié le 26/10/2026. Offre affichée dans la liste publique ANETI International. Consultez l'annonce originale pour le profil, les compétences et les modalités.",
                "https://www.aneti-international.tn/node/1067631", "26/10/2026"));
        add(new Job("Soudeur certifié aéronautique", "ANETI — Emploi international", "France", "International",
                "Mécanique • Soudure", "CDI",
                "Publié le 26/10/2026. Offre affichée dans la liste publique ANETI International. Consultez l'annonce originale pour les certifications demandées, le profil et les modalités.",
                "https://www.aneti-international.tn/node/1067652", "26/10/2026"));
        add(new Job("Ajusteurs monteurs aéronautique", "ANETI — Emploi international", "France", "International",
                "Aéronautique • Industrie", "CDI",
                "Publié le 28/10/2026. Offre affichée dans la liste publique ANETI International. Consultez l'annonce originale pour le profil, les compétences et les modalités.",
                "https://www.aneti-international.tn/node/1069379", "28/10/2026"));
    }};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (getSharedPreferences("tun_jobs", MODE_PRIVATE).getBoolean("welcome_done", false)) showHome();
        else showWelcome();
        if (!SecureSessionStore.read(this).isEmpty()) loadRemoteCandidateProfile();
    }

    private void prepareScreen(boolean rootScreen, Runnable action) {
        pendingRootScreen = rootScreen;
        pendingScreenAction = action;
    }

    @Override public void onBackPressed() {
        if (!screenHistory.isEmpty()) {
            Runnable previous = screenHistory.remove(screenHistory.size() - 1);
            restoringScreen = true;
            previous.run();
            restoringScreen = false;
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Quitter TUN-JOBS ?")
                .setMessage("Voulez-vous vraiment fermer l'application ?")
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Quitter", (dialog, which) -> finish())
                .show();
    }
    // ------------------------------------------------------------------ helpers
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean isArabic() {
        return "ar".equals(getSharedPreferences("tun_jobs", MODE_PRIVATE).getString("language", "fr"));
    }

    private String tr(String value) {
        if (value == null) return null;
        if ("en".equals(getSharedPreferences("tun_jobs", MODE_PRIVATE).getString("language", "fr"))) {
            switch (value) {
                case "Bienvenue sur TUN-JOBS": return "Welcome to TUN-JOBS";
                case "L'emploi et le recrutement, partout en Tunisie.": return "Jobs and recruitment, across Tunisia.";
                case "Votre prochain emploi peut commencer ici.": return "Your next job can start here.";
                case "Trouvez des opportunités près de chez vous, consultez les sources officielles et préparez votre profil candidat. Les employeurs peuvent aussi préparer leurs offres.": return "Find opportunities nearby, browse official sources and build your candidate profile. Employers can prepare job offers too.";
                case "Je cherche un emploi": return "I'm looking for a job";
                case "Je propose un emploi": return "I'm offering a job";
                case "Se connecter": return "Log in";
                case "Créer un compte": return "Sign up";
                case "Ou découvrir TUN-JOBS sans compte": return "Or explore TUN-JOBS without an account";
                case "Langue de l'application": return "App language";
                case "Changer la langue": return "Change language";
                case "Votre carte candidat": return "Your candidate profile";
                case "Un profil clair pour présenter votre parcours professionnel.": return "A clear profile to present your professional background.";
                case "Aperçu de votre profil": return "Profile preview";
                case "Modifier mes informations": return "Edit my information";
                case "Nom et prénom": return "Full name";
                case "Téléphone": return "Phone";
                case "Adresse e-mail": return "Email address";
                case "Ville / gouvernorat": return "City / governorate";
                case "Expérience et métier recherché": return "Experience and desired job";
                case "Formation / diplôme": return "Education / qualification";
                case "Compétences": return "Skills";
                case "Disponibilité": return "Availability";
                case "Nom et prénom *": return "Full name *";
                case "Téléphone *": return "Phone *";
                case "E-mail": return "Email";
                case "Mot de passe": return "Password";
                case "Mot de passe (6 caractères minimum)": return "Password (at least 6 characters)";
                case "Ajouter / modifier ma photo": return "Add / change my photo";
                case "Joindre mon CV (PDF ou document)": return "Attach my CV";
                case "Aucun CV sélectionné": return "No CV selected";
                case "CV enregistré sur cet appareil": return "CV saved on this device";
                case "CV synchronisé dans votre espace privé": return "CV synced to your private space";
                case "Enregistrer mon profil sur ce téléphone": return "Save my profile on this phone";
                case "Mes offres sauvegardées": return "My saved jobs";
                case "Aucune offre sauvegardée pour le moment.": return "No saved jobs yet.";
                case "Paramètres": return "Settings";
                case "Préférences de TUN-JOBS sur cet appareil.": return "TUN-JOBS preferences on this device.";
                case "Confidentialité": return "Privacy";
                case "Retour aux emplois": return "Back to jobs";
                case "Accueil": return "Home";
                case "Offres": return "Jobs";
                case "Profil": return "Profile";
                case "Employeurs": return "Employers";
                case "Plus": return "More";
                case "Menu TUN-JOBS": return "TUN-JOBS menu";
                case "À propos": return "About";
                case "Sites officiels de l'emploi": return "Official employment websites";
                case "Bureaux régionaux": return "Regional offices";
                case "Retour": return "Back";
                case "✓ SYNCHRONISÉ EN LIGNE": return "✓ SYNCED ONLINE";
                case "⚠ ÉCHEC DE SYNCHRONISATION": return "⚠ SYNC FAILED";
                case "● ENREGISTRÉ LOCALEMENT • EN ATTENTE": return "● SAVED LOCALLY • PENDING";
                case "APERÇU PRIVÉ • À COMPLÉTER": return "PRIVATE PREVIEW • INCOMPLETE";
                case "E-mail et CV restent masqués dans cet aperçu public.": return "Email and CV are hidden from this public preview.";
                case "Français": return "French";
                case "English": return "English";
                case "العربية": return "Arabic";
                default: break;
            }
        }
        if (!isArabic()) return value;
        switch (value) {
            case "Bienvenue sur TUN-JOBS": return "مرحباً بك في TUN-JOBS";
            case "L'emploi et le recrutement, partout en Tunisie.": return "التشغيل والانتداب في كامل تونس";
            case "Votre prochain emploi peut commencer ici.": return "فرصتك المهنية القادمة تبدأ من هنا";
            case "Trouvez des opportunités près de chez vous, consultez les sources officielles et préparez votre profil candidat. Les employeurs peuvent aussi préparer leurs offres.": return "اكتشف فرص العمل بالقرب منك، وتصفّح المصادر الرسمية وأنشئ ملفك المهني. ويمكن للمؤسسات إعداد عروضها أيضاً.";
            case "Je cherche un emploi": return "أبحث عن عمل";
            case "Je propose un emploi": return "أعرض وظيفة";
            case "Ou découvrir TUN-JOBS sans compte": return "أو استكشف TUN-JOBS دون حساب";
            case "Se connecter": return "تسجيل الدخول";
            case "Créer un compte": return "إنشاء حساب";
            case "Langue de l'application": return "لغة التطبيق";
            case "Changer la langue": return "تغيير اللغة";
            case "Votre carte candidat": return "بطاقتك المهنية";
            case "Un profil clair pour présenter votre parcours professionnel.": return "ملف مهني واضح لعرض خبراتك ومسارك";
            case "Aperçu de votre profil": return "معاينة ملفك الشخصي";
            case "Modifier mes informations": return "تعديل معلوماتي";
            case "Nom et prénom": return "الاسم واللقب";
            case "Téléphone": return "الهاتف";
            case "Adresse e-mail": return "البريد الإلكتروني";
            case "Ville / gouvernorat": return "المدينة / الولاية";
            case "Expérience et métier recherché": return "الخبرة والمهنة المطلوبة";
            case "Formation / diplôme": return "التكوين / الشهادة";
            case "Compétences": return "المهارات";
            case "Disponibilité": return "التوفر";
            case "Nom et prénom *": return "الاسم واللقب *";
            case "Téléphone *": return "الهاتف *";
            case "E-mail": return "البريد الإلكتروني";
            case "Mot de passe": return "كلمة المرور";
            case "Mot de passe (6 caractères minimum)": return "كلمة المرور (6 أحرف على الأقل)";
            case "Ajouter / modifier ma photo": return "إضافة / تغيير صورتي";
            case "Joindre mon CV (PDF ou document)": return "إرفاق سيرتي الذاتية";
            case "Aucun CV sélectionné": return "لم يتم اختيار سيرة ذاتية";
            case "CV synchronisé dans votre espace privé": return "تمت مزامنة السيرة الذاتية في مساحتك الخاصة";
            case "CV enregistré sur cet appareil": return "تم حفظ السيرة الذاتية على هذا الجهاز";
            case "Enregistrer mon profil sur ce téléphone": return "حفظ ملفي على هذا الهاتف";
            case "Mes offres sauvegardées": return "عروض العمل المحفوظة";
            case "Aucune offre sauvegardée pour le moment.": return "لا توجد عروض محفوظة حالياً";
            case "Paramètres": return "الإعدادات";
            case "Préférences de TUN-JOBS sur cet appareil.": return "إعدادات TUN-JOBS على هذا الجهاز";
            case "Confidentialité": return "الخصوصية";
            case "Retour aux emplois": return "العودة إلى الوظائف";
            case "Accueil": return "الرئيسية";
            case "Offres": return "العروض";
            case "Profil": return "الملف الشخصي";
            case "Employeurs": return "أصحاب العمل";
            case "Plus": return "المزيد";
            case "Menu TUN-JOBS": return "قائمة TUN-JOBS";
            case "À propos": return "حول التطبيق";
            case "Sites officiels de l'emploi": return "مواقع التشغيل الرسمية";
            case "Bureaux régionaux": return "المكاتب الجهوية";
            case "Retour": return "رجوع";
            case "✓ SYNCHRONISÉ EN LIGNE": return "✓ تمت المزامنة";
            case "⚠ ÉCHEC DE SYNCHRONISATION": return "⚠ فشلت المزامنة";
            case "● ENREGISTRÉ LOCALEMENT • EN ATTENTE": return "● محفوظ محلياً • في انتظار المزامنة";
            case "APERÇU PRIVÉ • À COMPLÉTER": return "معاينة خاصة • يرجى الإكمال";
            case "Français": return "الفرنسية";
            case "English": return "الإنجليزية";
            case "العربية": return "العربية";
            case "E-mail et CV restent masqués dans cet aperçu public.": return "يبقى البريد الإلكتروني والسيرة الذاتية مخفيين في هذه المعاينة العامة.";
            default: return value;
        }
    }

    private void selectLanguage(Runnable refresh) {
        String[] labels = {"Français", "English", "العربية"};
        String[] codes = {"fr", "en", "ar"};
        String current = getSharedPreferences("tun_jobs", MODE_PRIVATE).getString("language", "fr");
        int selected = "ar".equals(current) ? 2 : ("en".equals(current) ? 1 : 0);
        new AlertDialog.Builder(this)
                .setTitle("Language / Langue / اللغة")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    getSharedPreferences("tun_jobs", MODE_PRIVATE).edit().putString("language", codes[which]).apply();
                    dialog.dismiss();
                    refresh.run();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void openAccountFromWelcome(boolean create) {
        getSharedPreferences("tun_jobs", MODE_PRIVATE).edit().putBoolean("welcome_done", true).apply();
        preferSignupOnAccount = create;
        showAccount();
    }

    private GradientDrawable shape(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private GradientDrawable goldGradient(int radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Color.rgb(255, 204, 75), Color.rgb(222, 145, 8)});
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private void space(LinearLayout parent, int heightDp) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private void hspace(LinearLayout parent, int widthDp) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(dp(widthDp), 1));
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(tr(label));
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setGravity(Gravity.CENTER);
        b.setStateListAnimator(null);
        b.setTextColor(primary ? Color.rgb(20, 20, 22) : ink);
        b.setBackground(primary ? goldGradient(26) : shape(Color.rgb(39, 41, 47), 26));
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private Button segment(String label, boolean active, Runnable action) {
        Button b = button(label, active, action);
        b.setTextSize(14);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        if (!active) {
            b.setBackground(null);
            b.setTextColor(muted);
        }
        return b;
    }

    private TextView chip(String label, boolean highlight) {
        TextView c = text(label, 12, highlight ? Color.rgb(20, 20, 22) : Color.WHITE, true);
        c.setGravity(Gravity.CENTER);
        c.setBackground(shape(highlight ? gold : Color.rgb(61, 63, 69), 16));
        c.setPadding(dp(12), dp(6), dp(12), dp(6));
        return c;
    }

    private TextView topIcon(String glyph, int sizeSp, Runnable action) {
        TextView t = new TextView(this);
        t.setText(glyph);
        t.setTextSize(sizeSp);
        t.setTextColor(ink);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(v -> action.run());
        return t;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(tr(value));
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir le lien.", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------ screen frame
    private void base(String title, String subtitle) {
        base(title, subtitle, TAB_MORE, true);
    }

    /** tab < 0 hides the bottom navigation; title == null hides the title block. */
    private void base(String title, String subtitle, int tab, boolean scrollable) {
        profileVisible = false;
        if (pendingRootScreen) {
            screenHistory.clear();
        } else if (!restoringScreen && currentScreenAction != null) {
            screenHistory.add(currentScreenAction);
        }
        if (pendingScreenAction != null) currentScreenAction = pendingScreenAction;
        pendingScreenAction = null;
        pendingRootScreen = false;

        root = column();
        root.setLayoutDirection(isArabic() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setTextDirection(isArabic() ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        root.setBackgroundColor(canvas);
        setContentView(root);
        getWindow().setStatusBarColor(canvas);
        getWindow().setNavigationBarColor(canvas);
        // light icons on our dark background
        getWindow().getDecorView().setSystemUiVisibility(0);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });

        // top bar: menu - logo - search - notifications
        LinearLayout top = row();
        top.setPadding(dp(8), dp(6), dp(8), dp(6));
        top.setBackgroundColor(Color.rgb(8, 9, 12));
        TextView menuIcon = topIcon("☰", 26, this::showMenu);
        menuIcon.setContentDescription("Menu");
        top.addView(menuIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout brandBox = column();
        SpannableString brandText = new SpannableString("TUN-JOBS");
        brandText.setSpan(new ForegroundColorSpan(gold), 4, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView brand = text("", 24, Color.WHITE, true);
        brand.setText(brandText);
        brandBox.addView(brand);
        brandBox.addView(text("Trouvez votre emploi en Tunisie", 12, muted, false));
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(0, -2, 1);
        brandParams.leftMargin = dp(6);
        top.addView(brandBox, brandParams);
        TextView searchIcon = topIcon("🔍", 20, this::showOffers);
        searchIcon.setContentDescription("Rechercher");
        top.addView(searchIcon, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView bellIcon = topIcon("🔔", 20, () ->
                Toast.makeText(this, "Aucune nouvelle notification.", Toast.LENGTH_SHORT).show());
        bellIcon.setContentDescription("Notifications");
        top.addView(bellIcon, new LinearLayout.LayoutParams(dp(44), dp(48)));
        root.addView(top);

        if (title != null) {
            LinearLayout head = column();
            head.setPadding(dp(18), dp(10), dp(18), dp(4));
            head.addView(text(title, 22, ink, true));
            if (subtitle != null && !subtitle.isEmpty()) {
                space(head, 2);
                head.addView(text(subtitle, 13, muted, false));
            }
            root.addView(head);
        }

        content = column();
        if (scrollable) {
            ScrollView scroll = new ScrollView(this);
            content.setPadding(dp(16), dp(12), dp(16), dp(24));
            scroll.setFillViewport(true);
            scroll.setClipToPadding(false);
            scroll.addView(content);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            content.setPadding(dp(14), dp(4), dp(14), dp(6));
            root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        if (tab >= 0) root.addView(buildNav(tab));
    }

    private View buildNav(int active) {
        LinearLayout nav = row();
        nav.setBackgroundColor(Color.rgb(12, 13, 17));
        nav.setPadding(0, dp(4), 0, dp(6));
        String[] icons = {"🏠", "💼", "👤", "🏢", "⚙️"};
        String[] labels = {"Accueil", "Offres", "Profil", "Employeurs", "Plus"};
        Runnable[] actions = {this::showHome, this::showOffers, this::showProfile, this::showEmployerSpace, this::showMenu};
        for (int i = 0; i < 5; i++) {
            boolean on = i == active;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            View bar = new View(this);
            if (on) bar.setBackground(shape(gold, 2));
            item.addView(bar, new LinearLayout.LayoutParams(dp(26), dp(3)));
            TextView icon = text(icons[i], 20, Color.WHITE, false);
            icon.setGravity(Gravity.CENTER);
            icon.setAlpha(on ? 1f : 0.55f);
            item.addView(icon, new LinearLayout.LayoutParams(-2, dp(30)));
            TextView label = text(labels[i], 11, on ? gold : muted, on);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            item.addView(label, new LinearLayout.LayoutParams(-2, -2));
            final Runnable action = actions[i];
            item.setOnClickListener(v -> action.run());
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(62), 1));
        }
        return nav;
    }

    private void saveJob(Job job) {
        android.content.SharedPreferences prefs = getSharedPreferences("gass_jobs_saved", MODE_PRIVATE);
        Set<String> saved = new HashSet<String>(prefs.getStringSet("keys", new HashSet<String>()));
        boolean added = saved.add(job.key());
        prefs.edit().putStringSet("keys", saved).apply();
        Toast.makeText(this, added ? "Offre enregistrée dans votre profil." : "Offre déjà enregistrée.",
                Toast.LENGTH_SHORT).show();
    }

    private void infoCard(String title, String detail, String actionLabel, Runnable action) {
        LinearLayout c = column();
        c.setPadding(dp(17), dp(15), dp(17), dp(15));
        c.setBackground(shape(panel, 18));
        c.addView(text(title, 18, ink, true));
        space(c, 8);
        c.addView(text(detail, 14, muted, false));
        space(c, 12);
        c.addView(button(actionLabel, true, action));
        content.addView(c);
        space(content, 12);
    }

    private Spinner filterSpinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, values) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getView(position, convertView, parent);
                t.setTextColor(ink);
                t.setTextSize(15);
                t.setPadding(dp(14), dp(10), dp(14), dp(10));
                return t;
            }

            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getDropDownView(position, convertView, parent);
                t.setTextColor(ink);
                t.setTextSize(15);
                t.setBackgroundColor(panel);
                t.setPadding(dp(16), dp(14), dp(16), dp(14));
                return t;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPopupBackgroundDrawable(shape(panel, 12));
        return spinner;
    }

    /** Adds a label + dark rounded spinner box to the current content column. */
    private void addSpinner(String label, Spinner spinner) {
        content.addView(text(label, 13, muted, true));
        space(content, 4);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(shape(panel, 14));
        spinner.setBackground(null);
        box.addView(spinner, new FrameLayout.LayoutParams(-1, -1));
        TextView arrow = text("▾", 18, muted, true);
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL);
        arrowParams.rightMargin = dp(14);
        box.addView(arrow, arrowParams);
        content.addView(box, new LinearLayout.LayoutParams(-1, dp(48)));
        space(content, 10);
    }

    private String selected(Spinner spinner) {
        Object value = spinner.getSelectedItem();
        return value == null ? "Tous" : value.toString();
    }

    // ------------------------------------------------------------- swipe deck
    private int[] palette(String seed) {
        int[][] palettes = {
            {Color.rgb(84, 64, 26), Color.rgb(36, 29, 20), Color.rgb(12, 12, 14)},
            {Color.rgb(24, 70, 88), Color.rgb(14, 32, 42), Color.rgb(10, 12, 16)},
            {Color.rgb(70, 42, 94), Color.rgb(28, 19, 42), Color.rgb(11, 10, 16)},
            {Color.rgb(26, 80, 60), Color.rgb(14, 36, 29), Color.rgb(10, 14, 13)},
            {Color.rgb(92, 42, 42), Color.rgb(38, 19, 21), Color.rgb(14, 10, 11)}
        };
        return palettes[Math.abs(seed.hashCode() % palettes.length)];
    }

    private String sectorGlyph(String sector) {
        if ("Commerce".equals(sector)) return "🛒";
        if ("Informatique".equals(sector)) return "💻";
        if ("Agriculture".equals(sector)) return "🌾";
        if ("Bâtiment".equals(sector)) return "🏗️";
        if ("Santé".equals(sector)) return "🩺";
        if ("Transport".equals(sector)) return "🚚";
        return "💼";
    }

    private TextView stamp(String label, int color, int rotation) {
        TextView t = text(label, 22, color, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(4), dp(12), dp(4));
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.argb(60, 0, 0, 0));
        g.setCornerRadius(dp(8));
        g.setStroke(dp(3), color);
        t.setBackground(g);
        t.setRotation(rotation);
        t.setAlpha(0f);
        return t;
    }

    private FrameLayout deckCard(int[] colors, String badge, String place, String glyph, String title,
                                 String line1, String line2, String desc, String[] chips, String counter) {
        FrameLayout card = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        bg.setCornerRadius(dp(28));
        card.setBackground(bg);
        card.setClipToOutline(true);

        TextView avatar = text(glyph, 52, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(circle(Color.argb(46, 255, 255, 255)));
        FrameLayout.LayoutParams avatarParams = new FrameLayout.LayoutParams(dp(112), dp(112), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        avatarParams.topMargin = dp(64);
        card.addView(avatar, avatarParams);

        TextView badgeView = text(badge, 12, Color.WHITE, true);
        badgeView.setBackground(shape(Color.rgb(24, 128, 82), 16));
        badgeView.setPadding(dp(12), dp(6), dp(12), dp(6));
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        badgeParams.setMargins(dp(16), dp(16), 0, 0);
        card.addView(badgeView, badgeParams);

        TextView placeView = text("📍 " + place, 13, Color.rgb(20, 20, 22), true);
        placeView.setBackground(shape(Color.argb(240, 255, 255, 255), 16));
        placeView.setPadding(dp(12), dp(6), dp(12), dp(6));
        FrameLayout.LayoutParams placeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        placeParams.setMargins(0, dp(16), dp(16), 0);
        card.addView(placeView, placeParams);

        TextView counterView = text(counter, 12, Color.WHITE, true);
        counterView.setBackground(shape(Color.argb(150, 0, 0, 0), 14));
        counterView.setPadding(dp(10), dp(5), dp(10), dp(5));
        FrameLayout.LayoutParams counterParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        counterParams.setMargins(0, dp(58), dp(16), 0);
        card.addView(counterView, counterParams);

        LinearLayout info = column();
        info.setPadding(dp(18), dp(80), dp(18), dp(18));
        info.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {Color.argb(0, 0, 0, 0), Color.argb(215, 0, 0, 0), Color.argb(245, 0, 0, 0)}));
        info.addView(text(title, 26, Color.WHITE, true));
        space(info, 6);
        info.addView(text(line1, 16, Color.rgb(255, 214, 120), true));
        space(info, 3);
        info.addView(text(line2, 14, Color.rgb(225, 230, 238), false));
        space(info, 10);
        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(70, 255, 255, 255));
        info.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        space(info, 10);
        TextView descView = text(desc, 14, Color.rgb(235, 238, 244), false);
        descView.setMaxLines(2);
        descView.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(descView);
        space(info, 12);
        LinearLayout chipRow = row();
        for (int i = 0; i < chips.length && i < 3; i++) {
            TextView c = text(chips[i], 12, i == 0 ? Color.rgb(20, 20, 22) : Color.WHITE, true);
            c.setGravity(Gravity.CENTER);
            c.setSingleLine(true);
            c.setEllipsize(TextUtils.TruncateAt.END);
            c.setBackground(shape(i == 0 ? gold : Color.argb(120, 70, 74, 84), 16));
            c.setPadding(dp(8), dp(7), dp(8), dp(7));
            if (i > 0) hspace(chipRow, 6);
            chipRow.addView(c, new LinearLayout.LayoutParams(0, -2, 1));
        }
        info.addView(chipRow);
        card.addView(info, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        likeStamp = stamp("ENREGISTRER", Color.rgb(46, 204, 113), -14);
        FrameLayout.LayoutParams likeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        likeParams.setMargins(dp(20), dp(110), 0, 0);
        card.addView(likeStamp, likeParams);
        nopeStamp = stamp("PASSER", Color.rgb(255, 82, 82), 14);
        FrameLayout.LayoutParams nopeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        nopeParams.setMargins(0, dp(110), dp(20), 0);
        card.addView(nopeStamp, nopeParams);
        return card;
    }

    private View jobCard(Job job, int index, int total) {
        String badge = job.isOfficial() ? "✓ ANETI • OFFRE OFFICIELLE" : "● Annonce de démonstration";
        String dateLine = job.publishedDate.isEmpty() ? "" : " • Publié le " + job.publishedDate;
        String distance = distanceLabel(job);
        if (!distance.isEmpty()) dateLine += " • " + distance;
        return deckCard(palette(job.sector), badge, job.location, sectorGlyph(job.sector),
                job.title, "🏢  " + job.company, "📍  " + job.location + dateLine,
                job.description, new String[] {job.sector, job.contract}, (index + 1) + "/" + total);
    }

    private View candidateCard(Candidate c, int index, int total) {
        String[] skills = c.skills.split(" • ");
        String[] chips = new String[Math.min(3, skills.length)];
        for (int i = 0; i < chips.length; i++) chips[i] = skills[i];
        return deckCard(palette(c.name), "✓ Profil de démonstration", c.city,
                c.name.substring(0, 1).toUpperCase(Locale.ROOT),
                c.name, "💼  " + c.profession, "🎓  " + c.education + " • " + c.experience,
                "● " + c.availability + " — profil fictif, aucun contact réel.", chips, (index + 1) + "/" + total);
    }

    private void fly(View card, int direction, Runnable after) {
        if (animating) return;
        animating = true;
        float distance = getResources().getDisplayMetrics().widthPixels * 1.2f;
        card.animate().translationX(direction * distance).rotation(direction * 18f).alpha(0f)
                .setDuration(220).withEndAction(after).start();
    }

    private void attachSwipe(final View card, final TextView like, final TextView nope,
                             final Runnable onLeft, final Runnable onRight, final Runnable onTap) {
        final float[] startX = {0f};
        final boolean[] moved = {false};
        card.setOnTouchListener((v, e) -> {
            if (animating) return true;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                startX[0] = e.getRawX();
                moved[0] = false;
                if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            float dx = e.getRawX() - startX[0];
            if (action == MotionEvent.ACTION_MOVE) {
                if (Math.abs(dx) > dp(8)) moved[0] = true;
                v.setTranslationX(dx);
                v.setRotation(dx / Math.max(1, v.getWidth()) * 14f);
                float progress = Math.min(1f, Math.abs(dx) / (Math.max(1, v.getWidth()) * 0.3f));
                like.setAlpha(dx > 0 ? progress : 0f);
                nope.setAlpha(dx < 0 ? progress : 0f);
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                if (Math.abs(dx) > v.getWidth() * 0.3f) {
                    fly(v, dx > 0 ? 1 : -1, dx > 0 ? onRight : onLeft);
                } else {
                    v.animate().translationX(0f).rotation(0f).setDuration(160).start();
                    like.setAlpha(0f);
                    nope.setAlpha(0f);
                    if (!moved[0]) onTap.run();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                v.animate().translationX(0f).rotation(0f).setDuration(160).start();
                like.setAlpha(0f);
                nope.setAlpha(0f);
            }
            return true;
        });
    }

    private void addAction(String glyph, String label, int color, int sizeDp, Runnable action) {
        LinearLayout col = column();
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView b = new TextView(this);
        b.setText(glyph);
        b.setTextSize(sizeDp >= 58 ? 26 : 22);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(circle(color));
        b.setContentDescription(label);
        b.setOnClickListener(v -> action.run());
        col.addView(b, new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        TextView l = text(label, 11, muted, false);
        l.setTextSize(10.5f);
        l.setSingleLine(true);
        l.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
        labelParams.topMargin = dp(4);
        col.addView(l, labelParams);
        actionBar.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private View emptyDeck(final boolean candidates) {
        LinearLayout box = column();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.setBackground(shape(panel, 28));
        TextView icon = text("✓", 54, gold, true);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon);
        space(box, 8);
        TextView msg = text(candidates ? "Vous avez parcouru tous les profils de démonstration."
                : "Vous avez parcouru toutes les offres ANETI chargées.", 18, ink, true);
        msg.setGravity(Gravity.CENTER);
        box.addView(msg);
        space(box, 18);
        box.addView(button("Recommencer", true, () -> {
            if (candidates) candidateSwipeIndex = 0; else swipeIndex = 0;
            renderDeck();
        }), new LinearLayout.LayoutParams(-1, dp(52)));
        if (!candidates) {
            space(box, 10);
            box.addView(button("Voir les offres ANETI en Tunisie", false,
                    () -> openExternal(ANETI_TUNISIA_OFFERS)), new LinearLayout.LayoutParams(-1, dp(50)));
            space(box, 6);
            box.addView(button("Toutes les offres ANETI International", false,
                    () -> openExternal(ANETI_INTERNATIONAL_OFFERS)), new LinearLayout.LayoutParams(-1, dp(50)));
        }
        return box;
    }

    private void showCandidateDialog(Candidate c) {
        new AlertDialog.Builder(this)
                .setTitle(c.name + " — " + c.profession)
                .setMessage("Ville : " + c.city + "\nExpérience : " + c.experience
                        + "\nFormation : " + c.education + "\nCompétences : " + c.skills
                        + "\nDisponibilité : " + c.availability
                        + "\n\nProfil fictif de démonstration. Aucun contact réel n'est associé.")
                .setPositiveButton("Fermer", (dialog, which) -> dialog.dismiss()).show();
    }

    private void renderDeck() {
        animating = false;
        deckHolder.removeAllViews();
        actionBar.removeAllViews();
        final boolean candidates = employerMode;
        final int total = candidates ? demoCandidates.size() : anetiJobs.size();
        final int index = candidates ? candidateSwipeIndex : swipeIndex;
        if (index >= total) {
            deckHolder.addView(emptyDeck(candidates), new FrameLayout.LayoutParams(-1, -1));
            return;
        }
        if (index + 1 < total) {
            View behind = new View(this);
            behind.setBackground(shape(Color.rgb(26, 28, 34), 28));
            FrameLayout.LayoutParams behindParams = new FrameLayout.LayoutParams(-1, -1);
            behindParams.setMargins(dp(16), dp(16), dp(16), 0);
            deckHolder.addView(behind, behindParams);
        }
        final Job job = candidates ? null : anetiJobs.get(index);
        final Candidate person = candidates ? demoCandidates.get(index) : null;
        final View card = candidates ? candidateCard(person, index, total) : jobCard(job, index, total);
        final TextView like = likeStamp;
        final TextView nope = nopeStamp;
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(-1, -1);
        cardParams.bottomMargin = dp(14);
        deckHolder.addView(card, cardParams);

        final Runnable nextCard = () -> {
            if (candidates) candidateSwipeIndex++; else swipeIndex++;
            renderDeck();
        };
        final Runnable saveThenNext = () -> {
            if (candidates) saveCandidate(person); else saveJob(job);
            nextCard.run();
        };
        final Runnable details = () -> {
            if (candidates) showCandidateDialog(person);
            else if (job.isOfficial()) showOfficialJob(job);
            else showJob(job.title, job.company, job.location, job.description);
        };
        attachSwipe(card, like, nope, nextCard, saveThenNext, details);

        addAction("↺", "Retour", Color.rgb(52, 54, 62), 50, () -> {
            if (candidates) candidateSwipeIndex = Math.max(0, candidateSwipeIndex - 1);
            else swipeIndex = Math.max(0, swipeIndex - 1);
            renderDeck();
        });
        addAction("✕", "Ignorer", Color.rgb(229, 57, 53), 58, () -> {
            nope.setAlpha(1f);
            fly(card, -1, nextCard);
        });
        addAction("★", "Sauvegarder", Color.rgb(245, 178, 35), 58, () -> {
            like.setAlpha(1f);
            fly(card, 1, saveThenNext);
        });
        addAction("♥", "Contacter", Color.rgb(30, 170, 95), 58, () -> {
            if (candidates) {
                Toast.makeText(this, "Profil fictif : le contact sera disponible avec un compte sécurisé.", Toast.LENGTH_LONG).show();
            } else if (job.isOfficial()) {
                openExternal(job.sourceUrl);
            } else {
                showNotice();
            }
        });
        addAction("➤", "Partager", Color.rgb(35, 110, 230), 50, () -> {
            String message = candidates
                    ? "Découvrez TUN-JOBS, l'application emploi et recrutement en Tunisie."
                    : job.title + " — " + job.company + ", " + job.location
                      + (job.isOfficial() ? " • Offre publiée par ANETI : " + job.sourceUrl
                                          : " (annonce de démonstration)");
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(send, "Partager"));
        });
    }

    // ------------------------------------------------------------------ screens
    private void showHome() {
        prepareScreen(true, this::showHome);
        base(null, null, TAB_HOME, false);

        LinearLayout switcher = row();
        switcher.setBackground(shape(Color.rgb(22, 24, 30), 28));
        switcher.setPadding(dp(4), dp(4), dp(4), dp(4));
        switcher.addView(segment("♙  Cherche un emploi", !employerMode, () -> { employerMode = false; showHome(); }),
                new LinearLayout.LayoutParams(0, dp(44), 1));
        switcher.addView(segment("▣  Propose un emploi", employerMode, () -> { employerMode = true; showHome(); }),
                new LinearLayout.LayoutParams(0, dp(44), 1));
        content.addView(switcher, new LinearLayout.LayoutParams(-1, -2));
        space(content, 8);
        if (!employerMode) {
            content.addView(button("🇹🇳  Offres ANETI en Tunisie", false,
                    () -> openExternal(ANETI_TUNISIA_OFFERS)),
                    new LinearLayout.LayoutParams(-1, dp(46)));
            space(content, 6);
            content.addView(text("Cartes suivantes : offres ANETI International publiées par la source officielle. Vérifiez la disponibilité et les délais sur chaque annonce.", 11, muted, false));
        } else {
            content.addView(text("Profils candidats de démonstration — aucun profil réel n'est publié.", 11, muted, false));
        }
        space(content, 8);

        deckHolder = new FrameLayout(this);
        content.addView(deckHolder, new LinearLayout.LayoutParams(-1, 0, 1));
        space(content, 6);

        actionBar = row();
        actionBar.setGravity(Gravity.CENTER);
        actionBar.setPadding(0, dp(2), 0, dp(2));
        content.addView(actionBar, new LinearLayout.LayoutParams(-1, -2));
        renderDeck();
    }

    private void showCandidateDeck() {
        employerMode = true;
        showHome();
    }

    private Location coordinatesForPlace(String place) {
        if (place == null) return null;
        String p = place.toLowerCase(Locale.ROOT).trim();
        double lat, lon;
        if (p.contains("kasserine")) { lat=35.1676; lon=8.8365; }
        else if (p.contains("sbeitla")) { lat=35.2364; lon=9.1172; }
        else if (p.contains("feriana") || p.contains("fériana")) { lat=34.9543; lon=8.5689; }
        else if (p.contains("thala") || p.contains("tala")) { lat=35.5728; lon=8.6708; }
        else if (p.contains("kairouan")) { lat=35.6781; lon=10.0963; }
        else if (p.contains("sfax")) { lat=34.7406; lon=10.7603; }
        else if (p.contains("tunis")) { lat=36.8065; lon=10.1815; }
        else if (p.contains("sousse")) { lat=35.8256; lon=10.6084; }
        else if (p.contains("gafsa")) { lat=34.4250; lon=8.7842; }
        else if (p.contains("sidi bouzid")) { lat=35.0382; lon=9.4849; }
        else if (p.contains("gabes") || p.contains("gabès")) { lat=33.8815; lon=10.0982; }
        else if (p.contains("monastir")) { lat=35.7643; lon=10.8113; }
        else if (p.contains("bizerte")) { lat=37.2744; lon=9.8739; }
        else if (p.contains("nabeul")) { lat=36.4513; lon=10.7357; }
        else if (p.contains("beja") || p.contains("béja")) { lat=36.7256; lon=9.1817; }
        else if (p.contains("jendouba")) { lat=36.5011; lon=8.7802; }
        else if (p.contains("le kef") || p.equals("kef")) { lat=36.1742; lon=8.7049; }
        else if (p.contains("mahdia")) { lat=35.5047; lon=11.0622; }
        else if (p.contains("tozeur")) { lat=33.9197; lon=8.1335; }
        else if (p.contains("medenine") || p.contains("médenine")) { lat=33.3549; lon=10.5055; }
        else if (p.contains("tataouine")) { lat=32.9297; lon=10.4518; }
        else return null;
        Location point = new Location("tun-jobs-city"); point.setLatitude(lat); point.setLongitude(lon); return point;
    }

    private double distanceKm(Job job) {
        if (userLocation == null || job == null) return Double.NaN;
        Location destination = coordinatesForPlace(job.location);
        if (destination == null) destination = coordinatesForPlace(job.governorate);
        return destination == null ? Double.NaN : userLocation.distanceTo(destination) / 1000.0;
    }

    private String distanceLabel(Job job) {
        double km = distanceKm(job);
        return Double.isNaN(km) ? "" : String.format(Locale.getDefault(), "≈ %.1f km", km);
    }

    private void enableNearbySort() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }
        try {
            LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location last = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last == null) last = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null && System.currentTimeMillis() - last.getTime() < 86400000L) { applyUserLocation(last); return; }
            LocationListener listener = new LocationListener() {
                @Override public void onLocationChanged(Location value) {
                    try { manager.removeUpdates(this); } catch (SecurityException ignored) { }
                    applyUserLocation(value);
                }
                @Override public void onProviderEnabled(String provider) { }
                @Override public void onProviderDisabled(String provider) { }
                @Deprecated @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            };
            String provider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
            manager.requestLocationUpdates(provider, 0L, 0f, listener, getMainLooper());
            Toast.makeText(this, "Recherche de votre position…", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Position indisponible. Utilisez les filtres par gouvernorat.", Toast.LENGTH_LONG).show();
        }
    }

    private void applyUserLocation(Location value) {
        userLocation = value; sortByDistance = true;
        if (locationStatus != null) locationStatus.setText("📍 Position détectée — offres triées par distance approximative entre villes.");
        renderJobs();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            boolean allowed = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) allowed = true;
            if (allowed) enableNearbySort();
            else if (locationStatus != null) locationStatus.setText("Localisation refusée — vous pouvez continuer avec les filtres par gouvernorat.");
        }
    }

    private void renderJobs() {
        if (resultsContainer == null || searchInput == null) return;
        resultsContainer.removeAllViews();
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        String governorate = selected(governorateFilter);
        String sector = selected(sectorFilter);
        String contract = selected(contractFilter);
        visibleJobs.clear();
        for (Job job : demoJobs) {
            String searchable = (job.title + " " + job.company + " " + job.location + " " + job.governorate + " "
                    + job.sector + " " + job.contract + " " + job.description).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (!"Tous".equals(governorate) && !job.governorate.equalsIgnoreCase(governorate)) continue;
            if (!"Tous".equals(sector) && !job.sector.equalsIgnoreCase(sector)) continue;
            if (!"Tous".equals(contract) && !job.contract.equalsIgnoreCase(contract)) continue;
            visibleJobs.add(job);
        }
        if (sortByDistance && userLocation != null) Collections.sort(visibleJobs, new Comparator<Job>() {
            @Override public int compare(Job a, Job b) { return Double.compare(distanceKm(a), distanceKm(b)); }
        });
        resultsContainer.addView(text(visibleJobs.size() + " offre(s) trouvée(s)", 17, gold, true));
        space(resultsContainer, 2);
        resultsContainer.addView(text("Les annonces de démonstration sont fictives.", 12, muted, false));
        space(resultsContainer, 10);
        if (visibleJobs.isEmpty()) {
            resultsContainer.addView(text("Aucune offre ne correspond à ces filtres.", 15, muted, false));
            return;
        }
        for (final Job job : visibleJobs) {
            LinearLayout item = column();
            item.setPadding(dp(16), dp(14), dp(16), dp(14));
            item.setBackground(shape(panel, 18));
            item.addView(text(job.title, 18, ink, true));
            space(item, 3);
            String distance = distanceLabel(job);
            item.addView(text(job.company + " • " + job.location + (distance.isEmpty() ? "" : " • " + distance), 14, muted, false));
            space(item, 10);
            LinearLayout chips = row();
            chips.addView(chip(job.sector, true));
            hspace(chips, 8);
            chips.addView(chip(job.contract, false));
            item.addView(chips);
            space(item, 12);
            item.addView(button("Voir l'offre", true,
                    () -> showJob(job.title, job.company, job.location, job.description)),
                    new LinearLayout.LayoutParams(-1, dp(46)));
            resultsContainer.addView(item);
            space(resultsContainer, 10);
        }
    }

    private void showOffers() {
        prepareScreen(true, this::showOffers);
        base("Offres d'emploi", "Recherchez par métier, lieu, secteur ou contrat.", TAB_OFFERS, true);
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setHint("Métier, mot-clé ou entreprise");
        searchInput.setPadding(dp(16), dp(10), dp(16), dp(10));
        searchInput.setTextColor(ink);
        searchInput.setHintTextColor(muted);
        searchInput.setBackground(shape(panel, 14));
        content.addView(searchInput, new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 8);
        content.addView(button("📍  Offres les plus proches (GPS)", false, this::enableNearbySort), new LinearLayout.LayoutParams(-1, dp(48)));
        locationStatus = text("Autorisation demandée uniquement lorsque vous activez le tri GPS. Vous pouvez refuser et continuer sans localisation.", 12, muted, false);
        content.addView(locationStatus);
        space(content, 12);

        governorateFilter = filterSpinner(new String[] {
                "Tous", "Kasserine", "Tunis", "Ariana", "Ben Arous", "Manouba", "Nabeul",
                "Zaghouan", "Bizerte", "Béja", "Jendouba", "Le Kef", "Siliana", "Kairouan",
                "Sidi Bouzid", "Sousse", "Monastir", "Mahdia", "Sfax", "Gafsa", "Tozeur",
                "Kébili", "Gabès", "Médenine", "Tataouine"
        });
        addSpinner("Gouvernorat", governorateFilter);
        sectorFilter = filterSpinner(new String[] {
                "Tous", "Agriculture", "Bâtiment", "Commerce", "Informatique", "Santé", "Transport"
        });
        addSpinner("Secteur", sectorFilter);
        contractFilter = filterSpinner(new String[] {"Tous", "CDI", "CDD", "Temps plein", "Saisonnier"});
        addSpinner("Type de contrat", contractFilter);

        LinearLayout filterActions = row();
        filterActions.addView(button("Rechercher", true, this::renderJobs), new LinearLayout.LayoutParams(0, dp(48), 1));
        hspace(filterActions, 10);
        filterActions.addView(button("Effacer", false, () -> {
            searchInput.setText("");
            governorateFilter.setSelection(0);
            sectorFilter.setSelection(0);
            contractFilter.setSelection(0);
            renderJobs();
        }), new LinearLayout.LayoutParams(0, dp(48), 1));
        content.addView(filterActions);
        space(content, 18);
        resultsContainer = column();
        content.addView(resultsContainer);
        renderJobs();
        space(content, 8);
        content.addView(button("Trouver un bureau de l'emploi", false, this::showEmploymentOffices));
        space(content, 8);
        content.addView(button("Consulter les sources officielles d'emploi", false, this::showOfficialSources));
    }

    private void showEmployerSpace() {
        prepareScreen(true, this::showEmployerSpace);
        base("Espace recruteur", "Préparez vos offres et découvrez des profils.", TAB_EMPLOYERS, true);
        content.addView(text("Découvrez des profils candidats sous forme de cartes, puis préparez vos offres d'emploi.", 14, muted, false));
        space(content, 14);
        content.addView(button("♥  Découvrir les candidats", true, this::showCandidateDeck),
                new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 6);
        content.addView(text("Les profils affichés dans cette démonstration sont fictifs. Aucun candidat réel n'est publié ni contacté.", 12, muted, false));
        space(content, 18);
        content.addView(button("＋  Créer une offre", true, this::showEmployerDraft),
                new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 6);
        content.addView(text("Votre brouillon reste uniquement sur cet appareil : il n'est ni publié ni transmis.", 12, muted, false));
        space(content, 16);

        android.content.SharedPreferences draft = getSharedPreferences("gass_jobs_drafts", MODE_PRIVATE);
        if (draft.contains("title")) {
            LinearLayout offer = column();
            offer.setPadding(dp(18), dp(18), dp(18), dp(18));
            offer.setBackground(shape(Color.rgb(30, 33, 41), 22));
            offer.addView(text("APERÇU DE VOTRE OFFRE", 12, gold, true));
            space(offer, 12);
            offer.addView(text(draft.getString("title", "Votre poste"), 24, Color.WHITE, true));
            space(offer, 6);
            offer.addView(text(draft.getString("company", "Votre entreprise") + " • " + draft.getString("governorate", "Tunisie"), 14, Color.rgb(220, 230, 238), false));
            space(offer, 10);
            offer.addView(text(draft.getString("sector", "Secteur") + " • " + draft.getString("contract", "Contrat"), 13, gold, true));
            space(offer, 8);
            offer.addView(text(draft.getString("description", ""), 14, Color.WHITE, false));
            space(offer, 12);
            offer.addView(text("BROUILLON PRIVÉ • NON PUBLIÉ", 11, gold, true));
            content.addView(offer);
            space(content, 12);
            content.addView(button("Modifier mon offre", false, this::showEmployerDraft));
        } else {
            content.addView(text("Votre première annonce apparaîtra ici sous forme de carte. Elle restera privée jusqu'à la mise en place de la publication sécurisée.", 14, muted, false));
        }
        space(content, 14);
        content.addView(button("Voir les sources officielles", false, () -> openExternal(MINISTRY_DIRECTORY)));
    }

    private void saveCandidate(Candidate candidate) {
        android.content.SharedPreferences prefs = getSharedPreferences("tun_jobs_candidates", MODE_PRIVATE);
        Set<String> saved = new HashSet<String>(prefs.getStringSet("keys", new HashSet<String>()));
        boolean added = saved.add(candidate.key());
        prefs.edit().putStringSet("keys", saved).apply();
        Toast.makeText(this, added ? "Profil ajouté à la présélection." : "Profil déjà présélectionné.", Toast.LENGTH_SHORT).show();
    }

    private void showAccount() {
        prepareScreen(false, this::showAccount);
        android.content.SharedPreferences account = getSharedPreferences("tun_jobs_account", MODE_PRIVATE);
        String savedEmail = account.getString("email", "");
        String token = SecureSessionStore.read(this);
        base("Compte TUN-JOBS", "Connexion sécurisée avec votre compte Supabase.");
        if (!token.isEmpty()) {
            content.addView(chip("✓ CONNECTÉ", true));
            space(content, 10);
            content.addView(text("Adresse : " + savedEmail, 15, ink, true));
            space(content, 8);
            content.addView(text("Votre session est enregistrée sur cet appareil. La synchronisation des profils, offres et candidatures sera activée dans les prochaines étapes.", 13, muted, false));
            space(content, 16);
            content.addView(button("Se déconnecter", false, () -> {
                account.edit().clear().apply();
                SecureSessionStore.clear(this);
                Toast.makeText(this, "Déconnecté de TUN-JOBS.", Toast.LENGTH_SHORT).show();
                showAccount();
            }));
            return;
        }
        EditText email = profileField("Adresse e-mail", savedEmail);
        email.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = profileField("Mot de passe (6 caractères minimum)", "");
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(text("E-mail", 13, muted, true));
        content.addView(email, new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 10);
        content.addView(text("Mot de passe", 13, muted, true));
        content.addView(password, new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 12);
        if (preferSignupOnAccount) {
            content.addView(button("Créer un compte", true, () -> submitAuth(email, password, true)), new LinearLayout.LayoutParams(-1, dp(54)));
            space(content, 8);
            content.addView(button("Se connecter", false, () -> submitAuth(email, password, false)), new LinearLayout.LayoutParams(-1, dp(54)));
        } else {
            content.addView(button("Se connecter", true, () -> submitAuth(email, password, false)), new LinearLayout.LayoutParams(-1, dp(54)));
            space(content, 8);
            content.addView(button("Créer un compte", false, () -> submitAuth(email, password, true)), new LinearLayout.LayoutParams(-1, dp(54)));
        }
        preferSignupOnAccount = false;
        space(content, 12);
        content.addView(text("Après inscription, vérifiez votre boîte e-mail si la confirmation est demandée. N'utilisez pas le même mot de passe que vos autres services.", 12, muted, false));
    }

    private void syncCandidateProfile(String fullName, String phone, String email, String city, String profession,
                                      String education, String skills, String availability) {
        String token = SecureSessionStore.read(this);
        String userId = getSharedPreferences("tun_jobs_account", MODE_PRIVATE).getString("user_id", "");
        if (token.isEmpty() || userId.isEmpty()) {
            getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit().putString("sync_status", "pending").apply();
            Toast.makeText(this, "Profil enregistré localement. Connectez-vous pour le synchroniser.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Synchronisation du profil…", Toast.LENGTH_SHORT).show();
        authExecutor.execute(() -> {
            android.content.SharedPreferences assetPrefs = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE);
            String photoPath = assetPrefs.getString("photo_remote_path", "");
            String cvPath = assetPrefs.getString("cv_remote_path", "");
            SupabaseProfile.Result result = SupabaseProfile.upsert(
                    userId, token, fullName, phone, email, city, profession, education, skills, availability,
                    photoPath, cvPath);
            if (result.ok) {
                try { syncPendingProfileAssets(userId, token); }
                catch (Exception assetError) {
                    getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit()
                            .putString("asset_sync_status", "pending").apply();
                }
            }
            runOnUiThread(() -> {
                if (isFinishing()) return;
                getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit()
                        .putString("sync_status", result.ok ? "online" : "pending").apply();
                if (profileVisible) showProfile();
                new AlertDialog.Builder(this)
                        .setTitle(result.ok ? "Profil synchronisé" : "Synchronisation impossible")
                        .setMessage(result.message)
                        .setPositiveButton("OK", null).show();
            });
        });
    }

    private void loadRemoteCandidateProfile() {
        final String token = SecureSessionStore.read(this);
        final String userId = getSharedPreferences("tun_jobs_account", MODE_PRIVATE).getString("user_id", "");
        if (token.isEmpty() || userId.isEmpty()) return;
        authExecutor.execute(() -> {
            try {
                SupabaseProfile.ProfileData remote = SupabaseProfile.fetch(userId, token);
                if (remote == null) return;
                File downloadedPhoto = null;
                if (remote.photoPath != null && !remote.photoPath.isEmpty()) {
                    File target = new File(getFilesDir(), "profile-photo-cloud" + SupabaseStorage.extensionForPath(remote.photoPath));
                    if (SupabaseStorage.downloadPublicPhoto(remote.photoPath, target)) downloadedPhoto = target;
                }
                File downloadedCv = null;
                if (remote.cvPath != null && !remote.cvPath.isEmpty()) {
                    android.content.SharedPreferences local = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE);
                    String localCv = local.getString("cv_file", "");
                    if (localCv.isEmpty() || !new File(getFilesDir(), localCv).exists()) {
                        File target = new File(getFilesDir(), "profile-cv-cloud" + SupabaseStorage.extensionForPath(remote.cvPath));
                        if (SupabaseStorage.downloadPrivateCv(remote.cvPath, target, token)) downloadedCv = target;
                    }
                }
                final File cloudPhoto = downloadedPhoto;
                final File cloudCv = downloadedCv;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    android.content.SharedPreferences.Editor e = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit();
                    e.putString("name", remote.fullName).putString("phone", remote.phone)
                            .putString("email", remote.email).putString("city", remote.city).putString("experience", remote.profession)
                            .putString("education", remote.education).putString("skills", remote.skills)
                            .putString("availability", remote.availability)
                            .putString("photo_remote_path", remote.photoPath == null ? "" : remote.photoPath)
                            .putString("cv_remote_path", remote.cvPath == null ? "" : remote.cvPath)
                            .putBoolean("profile_saved", true).putString("sync_status", "online")
                            .putLong("profile_updated_at", System.currentTimeMillis());
                    if (cloudPhoto != null) e.putString("photo_file", cloudPhoto.getName());
                    if (cloudCv != null) e.putString("cv_file", cloudCv.getName());
                    e.apply();
                    if (profileVisible) showProfile();
                });
            } catch (Exception ignored) {
                // Keep the last local copy intact when offline or when the session needs re-authentication.
            }
        });
    }

    private void submitAuth(EditText emailField, EditText passwordField, boolean create) {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();
        if (email.isEmpty() || !email.contains("@") || password.length() < 6) {
            Toast.makeText(this, "Saisissez un e-mail valide et un mot de passe d'au moins 6 caractères.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, create ? "Création du compte…" : "Connexion…", Toast.LENGTH_SHORT).show();
        authExecutor.execute(() -> {
            SupabaseAuth.Result result = create ? SupabaseAuth.signUp(email, password) : SupabaseAuth.signIn(email, password);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (!result.ok) {
                    new AlertDialog.Builder(this).setTitle("Compte TUN-JOBS").setMessage(result.message).setPositiveButton("OK", null).show();
                    return;
                }
                if (!result.accessToken.isEmpty()) {
                    try {
                        SecureSessionStore.save(this, result.accessToken);
                        getSharedPreferences("tun_jobs_account", MODE_PRIVATE).edit()
                                .putString("user_id", result.userId)
                                .putString("email", email).apply();
                    } catch (Exception secureError) {
                        new AlertDialog.Builder(this).setTitle("Session non enregistrée")
                                .setMessage("La connexion a réussi, mais la session sécurisée n'a pas pu être enregistrée sur cet appareil.")
                                .setPositiveButton("OK", null).show();
                        return;
                    }
                } else {
                    getSharedPreferences("tun_jobs_account", MODE_PRIVATE).edit().putString("email", email).apply();
                }
                new AlertDialog.Builder(this).setTitle(create ? "Inscription" : "Connexion")
                        .setMessage(result.message).setPositiveButton("Continuer", (d, w) -> {
                            if (!result.accessToken.isEmpty()) loadRemoteCandidateProfile();
                            showAccount();
                        }).show();
            });
        });
    }

    private void showMenu() {
        prepareScreen(false, this::showMenu);
        base("Menu TUN-JOBS", "Accès rapide à votre espace et aux réglages.");
        LinearLayout banner = column();
        banner.setPadding(dp(20), dp(22), dp(20), dp(22));
        banner.setBackground(shape(Color.rgb(24, 26, 31), 18));
        banner.addView(text("TUN-JOBS", 25, Color.WHITE, true));
        space(banner, 6);
        banner.addView(text("EMPLOI • RECRUTEMENT • TUNISIE", 13, gold, true));
        space(banner, 8);
        banner.addView(text("GASS-OS  •  BY AYMEN GHARSALLI", 13, Color.WHITE, false));
        content.addView(banner);
        space(content, 18);
        android.content.SharedPreferences account = getSharedPreferences("tun_jobs_account", MODE_PRIVATE);
        String signedEmail = account.getString("email", "");
        content.addView(text(signedEmail.isEmpty() ? "Aucun compte connecté" : "Connecté : " + signedEmail, 13, muted, false));
        space(content, 8);
        content.addView(button(signedEmail.isEmpty() ? "☁  Connexion / Créer un compte" : "☁  Gérer mon compte en ligne", true, this::showAccount), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("⌂   Emplois", false, this::showHome), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("♙   Mon profil", false, this::showProfile), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("⚙   Paramètres", false, this::showSettings), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("ⓘ   À propos", false, this::showAboutMe), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("Sites officiels de l'emploi", false, this::showOfficialSources), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("Bureaux régionaux", false, this::showEmploymentOffices), new LinearLayout.LayoutParams(-1, dp(58)));
    }

    private void showSettings() {
        prepareScreen(false, this::showSettings);
        base("Paramètres", "Préférences de TUN-JOBS sur cet appareil.");
        android.content.SharedPreferences prefs = getSharedPreferences("tun_jobs", MODE_PRIVATE);
        content.addView(text("Langue de l'application", 20, ink, true));
        space(content, 8);
        content.addView(button("Changer la langue", false, () -> selectLanguage(this::showSettings)),
                new LinearLayout.LayoutParams(-1, dp(54)));
        space(content, 16);
        content.addView(text("Confidentialité", 18, ink, true));
        space(content, 6);
        content.addView(text("Les informations du profil sont synchronisées avec Supabase. La photo est publique (nom et ville visibles). L’e-mail et le CV restent privés ; l’accès est réservé au candidat et aux employeurs vérifiés ayant une offre publiée.", 14, muted, false));
        space(content, 16);
        content.addView(button("Réafficher l'écran de bienvenue au prochain lancement", false, () -> {
            prefs.edit().putBoolean("welcome_done", false).apply();
            Toast.makeText(this, "L'écran de bienvenue sera affiché au prochain lancement.", Toast.LENGTH_LONG).show();
        }), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 10);
        content.addView(button("Retour aux emplois", true, this::showHome), new LinearLayout.LayoutParams(-1, dp(58)));
    }

    private void showAboutMe() {
        prepareScreen(false, this::showAboutMe);
        base("À propos", "Le développeur, la mission et les projets GASS-OS");
        LinearLayout banner = column();
        banner.setPadding(dp(20), dp(22), dp(20), dp(22));
        banner.setBackground(shape(Color.rgb(22, 35, 52), 18));
        banner.addView(text("TUN-JOBS", 28, Color.WHITE, true));
        space(banner, 8);
        banner.addView(text("Une plateforme tunisienne pour rapprocher les candidats et les employeurs.", 16, Color.WHITE, false));
        banner.setOnClickListener(v -> showLongDescription("TUN-JOBS", "TUN-JOBS est une plateforme mobile pensée pour rapprocher les demandeurs d'emploi, les recruteurs et les sources officielles en Tunisie. Elle met l'accent sur un accès simple aux opportunités, les profils professionnels, la découverte des offres et une expérience mobile moderne."));
        banner.setOnLongClickListener(v -> { showLongDescription("TUN-JOBS", "TUN-JOBS est une plateforme mobile pensée pour rapprocher les demandeurs d'emploi, les recruteurs et les sources officielles en Tunisie. Elle met l'accent sur un accès simple aux opportunités, les profils professionnels, la découverte des offres et une expérience mobile moderne."); return true; });
        content.addView(banner);
        space(content, 20);

        content.addView(text("DÉVELOPPEUR & FONDATEUR", 13, gold, true));
        space(content, 7);
        content.addView(text("Aymen Gharsalli", 25, ink, true));
        space(content, 5);
        TextView role = text("Fondateur de GASS-OS • Développeur indépendant • Créateur de solutions Android, Linux et de jeux vidéo.", 15, muted, false);
        role.setOnLongClickListener(v -> { showLongDescription("À propos du développeur", "Aymen Gharsalli est un développeur indépendant et le fondateur de GASS-OS. Son travail explore les applications Android, les environnements Linux mobiles, les outils logiciels et les jeux vidéo. Il développe des projets pratiques avec une attention particulière à l'autonomie, aux performances, à l'accessibilité et à une expérience utilisateur soignée. TUN-JOBS s'inscrit dans cette démarche : construire une passerelle numérique utile entre les talents et les employeurs en Tunisie."); return true; });
        content.addView(role);
        space(content, 10);
        content.addView(text("GASS-OS conçoit des applications et expériences numériques : outils mobiles, solutions Linux, plateformes de services et jeux interactifs. L'objectif est de créer des produits utiles, modernes et accessibles, en privilégiant le contrôle de l'utilisateur, la transparence et le développement progressif.", 15, muted, false));
        space(content, 18);

        content.addView(text("ME RETROUVER EN LIGNE", 13, gold, true));
        space(content, 9);
        addSocialLink("⌘   GitHub — GASS-OS", "https://github.com/Gass-OS");
        addSocialLink("▶   YouTube — Luxtrust OS", "https://www.youtube.com/@Luxtrust-Os");
        addSocialLink("f   Facebook", "https://www.facebook.com/BMENLUXIII");
        addSocialLink("✉   gharsalli1994@gmail.com", "mailto:gharsalli1994@gmail.com");
        space(content, 18);

        content.addView(text("MES PROJETS", 22, ink, true));
        space(content, 5);
        content.addView(text("Touchez une carte pour ouvrir le projet. Maintenez votre doigt appuyé pour lire sa description détaillée. Les liens APK ouvrent les builds et téléchargements publiés par chaque projet.", 14, muted, false));
        space(content, 12);
        addPortfolioCard("GASS-NATIVE", "Environnement Linux mobile basé sur Debian, avec une expérience de bureau XFCE sur Android.", "GASS-NATIVE est un projet visant à rendre les outils Linux accessibles depuis Android. Il s'appuie sur Debian et XFCE dans un environnement mobile, avec une approche légère et contrôlée par l'utilisateur.", "https://github.com/Gass-OS/GassNative-", "https://github.com/Gass-OS/GassNative-/actions");
        addPortfolioCard("WINE-HUB", "Laboratoire Android pour les applications Windows et les technologies de compatibilité graphique.", "WINE-HUB rassemble un environnement de compatibilité Windows sur Android autour de Wine, DXVK et des pilotes graphiques compatibles. Le projet explore les performances et l'intégration des applications Windows sur appareils mobiles.", "https://github.com/Gass-OS/WINE-HUB", "https://github.com/Gass-OS/WINE-HUB/actions");
        addPortfolioCard("KASSERINE CITY", "Jeu mobile 3D inspiré de Kasserine, avec exploration urbaine et missions.", "Kasserine City est un jeu 3D mobile qui met en scène une ville inspirée de Kasserine. Le projet développe progressivement ses quartiers, ses lieux emblématiques, ses personnages, ses missions et son cycle jour/nuit.", "https://github.com/Gass-OS/Kasserine-City", "https://github.com/Gass-OS/Kasserine-City/actions");
        addPortfolioCard("LONG NIGHT", "Jeu d'horreur et de survie mobile avec exploration, ambiance nocturne et créatures.", "Long Night est une expérience d'horreur et de survie développée pour Android, avec une progression scénarisée, des environnements sombres, des personnages, des chiens et des mécaniques de furtivité et de survie.", "https://github.com/Gass-OS/long-night", "https://github.com/Gass-OS/long-night/actions");
        addPortfolioCard("TUN-JOBS", "Plateforme tunisienne de découverte d'emplois et de mise en relation candidats-employeurs.", "TUN-JOBS est l'application présentée ici. Elle rassemble les profils candidats, les offres, les sources officielles et les outils de contact dans une interface mobile pensée pour la Tunisie.", "https://github.com/Gass-OS/GASS-JOBS", "https://github.com/Gass-OS/GASS-JOBS/actions/workflows/android.yml");
        space(content, 12);
        content.addView(text("CONTACT & COLLABORATION", 13, gold, true));
        space(content, 7);
        content.addView(button("Envoyer un e-mail", true, () -> openExternal("mailto:gharsalli1994@gmail.com?subject=Contact%20GASS-OS")), new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 18);
        content.addView(button("Retour", false, this::showMenu), new LinearLayout.LayoutParams(-1, dp(58)));
    }

    private void addSocialLink(String label, String url) {
        Button link = button(label + "   ↗", false, () -> openExternal(url));
        link.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        link.setOnLongClickListener(v -> { showLongDescription("Lien de contact", label + "\\n\\n" + url + "\\n\\nTouchez pour ouvrir ce lien dans votre application ou navigateur."); return true; });
        content.addView(link, new LinearLayout.LayoutParams(-1, dp(54)));
        space(content, 7);
    }

    private void addPortfolioCard(String title, String summary, String details, String sourceUrl, String buildsUrl) {
        LinearLayout card = column();
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackground(shape(Color.rgb(24, 34, 49), 18));
        TextView name = text(title, 19, Color.WHITE, true);
        card.addView(name);
        space(card, 6);
        card.addView(text(summary, 14, Color.rgb(194, 201, 213), false));
        space(card, 12);
        LinearLayout actions = row();
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(button("Projet ↗", true, () -> openExternal(sourceUrl)),
                new LinearLayout.LayoutParams(0, dp(48), 1));
        hspace(actions, 8);
        actions.addView(button("APK / Builds ↗", false, () -> openExternal(buildsUrl)),
                new LinearLayout.LayoutParams(0, dp(48), 1));
        card.addView(actions);
        card.setOnClickListener(v -> openExternal(sourceUrl));
        card.setOnLongClickListener(v -> { showLongDescription(title, details + "\\n\\nProjet : " + sourceUrl + "\\nBuilds APK : " + buildsUrl); return true; });
        content.addView(card, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
        space(content, 12);
    }

    private void showLongDescription(String title, String details) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(details)
                .setPositiveButton("Fermer", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showOfficialSources() {
        prepareScreen(true, this::showOfficialSources);
        base("Offres officielles en Tunisie", "Accès direct aux portails publics — sans copie ni republication des annonces.");
        content.addView(text("Consultez les annonces directement sur les sites officiels. Les disponibilités, conditions et dates limites sont celles publiées par chaque organisme.", 14, muted, false));
        space(content, 16);
        infoCard("ANETI — Agence nationale pour l'emploi", "Portail national de l'emploi et des services aux demandeurs d'emploi et aux entreprises. Les offres doivent être vérifiées sur le site source.", "Ouvrir emploi.nat.tn", () -> openExternal("https://www.emploi.nat.tn/"));
        infoCard("Concours de la fonction publique", "Portail national des concours et recrutements publics. Vérifiez les conditions, pièces exigées et délais dans chaque avis.", "Ouvrir concours.gov.tn", () -> openExternal("https://www.concours.gov.tn/"));
        infoCard("Ministère de l'Emploi", "Informations officielles, directions régionales, programmes et actualités du ministère.", "Ouvrir le ministère", () -> openExternal("https://www.emploi.gov.tn/fr/"));
        infoCard("Ministère de l'Intérieur — recrutements", "Portail de recrutement et avis publiés par le ministère. Consultez chaque annonce pour les modalités officielles.", "Ouvrir le portail de recrutement", () -> openExternal("https://recrutementdaf.interieur.gov.tn/Daaf/home"));
        space(content, 6);
        content.addView(text("Transparence et fiabilité", 18, ink, true));
        space(content, 6);
        content.addView(text("Les cartes ANETI International du carrousel reprennent les informations visibles sur les fiches publiques consultées le 10 novembre 2026 et renvoient vers chaque annonce originale. Les offres nationales restent consultables sur le portail ANETI Tunisie. Aucun flux/API public ni droit de republication automatique n'est présumé : la synchronisation nationale sera activée après vérification technique et autorisation de réutilisation.", 13, muted, false));
        space(content, 10);
        content.addView(button("Voir les bureaux régionaux", true, this::showEmploymentOffices));
    }

    private void officeCard(String governorate, String director, String address, String phone, String email) {
        LinearLayout c = column();
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        c.setBackground(shape(panel, 18));
        c.addView(text(governorate, 19, ink, true));
        space(c, 7);
        c.addView(text("Responsable : " + director, 15, green, true));
        space(c, 8);
        c.addView(text(address, 15, muted, false));
        c.addView(text("Téléphone : " + phone, 15, muted, false));
        if (email != null && !email.isEmpty()) {
            c.addView(text("E-mail : " + email, 15, muted, false));
        }
        space(c, 14);
        LinearLayout actions = row();
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(button("Appeler", true, () -> openExternal("tel:" + phone.replace(" ", ""))),
                new LinearLayout.LayoutParams(0, dp(58), 1));
        if (email != null && !email.isEmpty()) {
            actions.addView(button("E-mail", false, () -> openExternal("mailto:" + email)),
                    new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        c.addView(actions);
        content.addView(c);
        space(content, 16);
    }

    private void showEmploymentOffices() {
        prepareScreen(true, this::showEmploymentOffices);
        base("Emploi dans les régions", "Directions régionales — les 24 gouvernorats.");
        content.addView(text("KASSERINE EN PREMIER", 13, green, true));
        space(content, 6);
        content.addView(text("Contacts régionaux officiels", 20, ink, true));
        space(content, 8);
        content.addView(text("Coordonnées publiées par le Ministère de l'Emploi et de la Formation professionnelle. Ces fiches concernent les directions régionales, pas les adresses individuelles de chaque bureau local. Page officielle consultée le 8 novembre 2026.", 13, muted, false));
        space(content, 14);

        String[][] regions = {
            {"Kasserine", "Mohamed Armi", "Rue 7 novembre, BP 111, 1200 Kasserine", "77 475 366", "Mohamed.Armi@mfpe.state.tn"},
            {"Tunis", "Sondes Gmir", "6, rue de Cologne, 1001 Tunis", "71 789 389", "sondes.gmir@mfpe.state.tn"},
            {"Ariana", "Faten Jerjini", "17 Bis, rue Ahmed Khabtheni, 2080 Ariana", "71 717 696", "faten.jerjini@mfpe.state.tn"},
            {"Ben Arous", "Adel Khelifi", "56, rue de la France, 2013 Ben Arous", "71 381 510", "adel.khelifi@mfpe.state.tn"},
            {"Manouba", "Mbarka Rgoubi", "5, rue Abou Taieb Almoutanebbi, 2010 Manouba", "70 604 133", "mbarka.ragoubi@mfpe.state.tn"},
            {"Nabeul", "Adel Zitouni", "33, rue de France, 8000 Nabeul", "72 285 913", "adel.zitouni@mfpe.state.tn"},
            {"Zaghouan", "Anis Lachheb", "Rue Narjess, 1100 Zaghouan", "72 675 588", "anis.lachheb@mfpe.state.tn"},
            {"Bizerte", "Hamouda Gharech", "169, rue Hassen Ennouri, 7000 Bizerte", "72 432 378", "hamouda.gharech@mfpe.state.tn"},
            {"Béja", "Hatem Raihani", "Avenue Habib Bourguiba, 9000 Béja", "78 456 177", "hatem.raihani@mfpe.state.tn"},
            {"Jendouba", "Mohamed Rahali", "9, avenue 9 Avril, 8100 Jendouba", "78 601 160", "mohamed.rahali@mfpe.state.tn"},
            {"Le Kef", "Jalel Twihri", "Rue Taha Hassine, 7120 Le Kef", "78 223 822", "jalel.twihri@mfpe.state.tn"},
            {"Siliana", "Atef Bouraoui", "Rue 31 Août, 6100 Siliana", "78 872 580", "atef.bouraoui@mfpe.state.tn"},
            {"Kairouan", "Abdellatif Abbassi", "16, rue Baiet Elhikma, 3100 Kairouan", "77 230 876", "abdellatif.abbassi@mfpe.state.tn"},
            {"Sidi Bouzid", "Aymen Boualagui", "51, rue Karra Hdid, 9100 Sidi Bouzid", "76 633 545", "aymen.bouallagui@mfpe.state.tn"},
            {"Sousse", "Tarek Mahjoub", "Rue Ksantina, 4003 Sousse", "73 221 600", "tarek.mahjoub@mfpe.state.tn"},
            {"Monastir", "Sawssen Touir", "Rue Ibn Sina, 5000 Monastir", "73 461 866", "sawssen.touir@mfpe.state.tn"},
            {"Mahdia", "Khaled Hadj Hassen", "Rue Bachir Sfar, BP 111, 5100 Mahdia", "73 696 421", ""},
            {"Sfax", "Noura Zidi", "Rue Jemelleddine Afghani, Bouderiar 1, Sfax", "74 222 592", "noura.zidi@mfpe.state.tn"},
            {"Gafsa", "Mohamed Sniha", "Rue Erroman Douali, 2100 Gafsa", "76 228 346", "sniha.mohamed@mfpe.state.tn"},
            {"Tozeur", "Omar Khmira", "Route de Nafta, 2200 Tozeur", "76 468 631", "omar.khmira@mfpe.state.tn"},
            {"Kébili", "Othman Mahmoudi", "Route de Gabès, 4200 Kébili", "75 490 682", "othman.mahmoudi@mfpe.state.tn"},
            {"Gabès", "Nizar Khaldi", "Rue Belkacem Bazmi, 6000 Gabès", "75 270 435", "nizar.khaldi@mfpe.state.tn"},
            {"Médenine", "Mchabet Meftah", "Rue Borma, 4100 Médenine", "75 641 915", "meftah.mchabet@mfpe.state.tn"},
            {"Tataouine", "Kilani Oun", "Avenue El Koufa, cité El Mahrajen, Tataouine", "75 844 994", "kilani.jabri@mfpe.state.tn"}
        };
        for (String[] region : regions) {
            officeCard(region[0], region[1], region[2], region[3], region[4]);
        }

        space(content, 8);
        content.addView(text("Sources et actualisation", 18, ink, true));
        space(content, 6);
        content.addView(text("Source principale : annuaire officiel des directions régionales du ministère. Les coordonnées peuvent changer ; vérifiez-les auprès du ministère avant un déplacement. Pour Kasserine, la page ministérielle indique 3 bureaux d'emploi, mais cette liste ne prétend pas fournir leurs trois adresses individuelles.", 13, muted, false));
        space(content, 10);
        content.addView(button("Ouvrir l'annuaire officiel du ministère", true, () -> openExternal(MINISTRY_DIRECTORY)));
        space(content, 16);
        content.addView(text("Jeu de données national complémentaire", 17, ink, true));
        space(content, 5);
        content.addView(text("Le catalogue national propose un fichier CSV et XLSX sous licence CC BY. Attention : les ressources sont datées du 7 mars 2023 et leur couverture temporelle indiquée est 2016–2019. Elles ne sont pas utilisées ici comme source de coordonnées actuelles sans revalidation.", 13, muted, false));
        space(content, 10);
        content.addView(button("Ouvrir le jeu de données officiel", false, () -> openExternal(OPEN_DATA_DIRECTORY)));
    }

    private EditText profileField(String hint, String value) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(16);
        field.setMinHeight(0);
        field.setMinimumHeight(0);
        field.setIncludeFontPadding(true);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setHint(tr(hint));
        field.setLayoutDirection(isArabic() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        field.setGravity(isArabic() ? Gravity.CENTER_VERTICAL | Gravity.RIGHT : Gravity.CENTER_VERTICAL);
        field.setText(value);
        field.setPadding(dp(16), dp(6), dp(16), dp(6));
        field.setTextColor(ink);
        field.setHintTextColor(muted);
        field.setBackground(shape(panel, 14));
        return field;
    }

    private void pickProfileFile(int requestCode, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (requestCode == PICK_PROFILE_CV) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
        } else intent.setType(mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    // Keep selected media in app-private storage so it remains readable after restart,
    // even if Android's document provider later revokes or changes the original URI.
    private String copyProfileFile(Uri source, String fileName) {
        File target = new File(getFilesDir(), fileName);
        try (InputStream in = getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(target, false)) {
            if (in == null) return "";
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            out.flush();
            return fileName;
        } catch (Exception e) {
            if (target.exists()) target.delete();
            return "";
        }
    }

    private Uri savedProfileFileUri(android.content.SharedPreferences prefs, String fileKey,
                                    String legacyUriKey, String privateName) {
        String stored = prefs.getString(fileKey, "");
        if (!stored.isEmpty()) {
            File file = new File(getFilesDir(), stored);
            if (file.exists()) return Uri.fromFile(file);
        }
        // Migrate profiles created by earlier builds that stored a document-provider URI.
        String legacy = prefs.getString(legacyUriKey, "");
        if (!legacy.isEmpty()) {
            String copied = copyProfileFile(Uri.parse(legacy), privateName);
            if (!copied.isEmpty()) {
                prefs.edit().putString(fileKey, copied).remove(legacyUriKey).commit();
                return Uri.fromFile(new File(getFilesDir(), copied));
            }
        }
        return null;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri selected = data.getData();
        String storedName = requestCode == PICK_PROFILE_PHOTO ? "profile-photo" : "profile-cv";
        String copied = copyProfileFile(selected, storedName);
        if (copied.isEmpty()) {
            Toast.makeText(this, "Impossible d'enregistrer le fichier. Choisissez un autre document.", Toast.LENGTH_LONG).show();
            return;
        }
        String selectedMime = getContentResolver().getType(selected);
        android.content.SharedPreferences.Editor edit = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit();
        if (requestCode == PICK_PROFILE_PHOTO) edit.putString("photo_file", copied)
                .putString("photo_mime", selectedMime == null ? "image/jpeg" : selectedMime)
                .remove("photo_uri").remove("photo_remote_path");
        if (requestCode == PICK_PROFILE_CV) edit.putString("cv_file", copied)
                .putString("cv_mime", selectedMime == null ? "application/pdf" : selectedMime)
                .remove("cv_uri").remove("cv_remote_path");
        if (!edit.commit()) {
            Toast.makeText(this, "Le fichier est copié, mais l'enregistrement du profil a échoué.", Toast.LENGTH_LONG).show();
            return;
        }
        Uri privateUri = Uri.fromFile(new File(getFilesDir(), copied));
        if (requestCode == PICK_PROFILE_PHOTO && profilePhotoView != null) {
            profilePhotoView.clearColorFilter();
            profilePhotoView.setImageURI(privateUri);
        }
        if (requestCode == PICK_PROFILE_CV && profileCvLabel != null) profileCvLabel.setText("CV enregistré sur cet appareil");
        Toast.makeText(this, requestCode == PICK_PROFILE_PHOTO
                ? "Photo enregistrée sur cet appareil." : "CV enregistré sur cet appareil.", Toast.LENGTH_SHORT).show();
        syncSelectedProfileAsset(copied, requestCode);
    }

    private void syncSelectedProfileAsset(String localName, int requestCode) {
        String token = SecureSessionStore.read(this);
        String userId = getSharedPreferences("tun_jobs_account", MODE_PRIVATE).getString("user_id", "");
        if (token.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "Fichier conservé sur le téléphone. Connectez-vous puis enregistrez votre profil pour le synchroniser.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Envoi du fichier vers votre espace sécurisé…", Toast.LENGTH_SHORT).show();
        authExecutor.execute(() -> {
            try {
                syncPendingProfileAssets(userId, token);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (profileVisible) showProfile();
                    Toast.makeText(this, requestCode == PICK_PROFILE_PHOTO
                            ? "Photo synchronisée dans le profil public."
                            : "CV synchronisé dans l'espace privé.", Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit()
                            .putString("asset_sync_status", "pending").apply();
                    Toast.makeText(this, "Fichier gardé sur le téléphone ; synchronisation impossible : "
                            + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void syncPendingProfileAssets(String userId, String token) throws Exception {
        android.content.SharedPreferences prefs = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE);
        String photoPath = prefs.getString("photo_remote_path", "");
        String cvPath = prefs.getString("cv_remote_path", "");
        String localPhoto = prefs.getString("photo_file", "");
        String localCv = prefs.getString("cv_file", "");
        String photoMime = prefs.getString("photo_mime", "image/jpeg");
        String cvMime = prefs.getString("cv_mime", "application/pdf");
        if (photoPath.isEmpty() && !localPhoto.isEmpty()) {
            File file = new File(getFilesDir(), localPhoto);
            if (file.isFile()) {
                photoPath = SupabaseStorage.upload(file, "candidate-photos", userId, token, photoMime);
                SupabaseProfile.updateAssetPaths(userId, token, photoPath, cvPath);
                prefs.edit().putString("photo_remote_path", photoPath).apply();
            }
        }
        if (cvPath.isEmpty() && !localCv.isEmpty()) {
            File file = new File(getFilesDir(), localCv);
            if (file.isFile()) {
                cvPath = SupabaseStorage.upload(file, "candidate-cvs", userId, token, cvMime);
                SupabaseProfile.updateAssetPaths(userId, token, photoPath, cvPath);
                prefs.edit().putString("cv_remote_path", cvPath).apply();
            }
        }
        prefs.edit().putString("asset_sync_status",
                (!photoPath.isEmpty() || !cvPath.isEmpty()) ? "online" : "pending").apply();
    }

    private void showWelcome() {
        prepareScreen(true, this::showWelcome);
        base("Bienvenue sur TUN-JOBS", "L'emploi et le recrutement, partout en Tunisie.", -1, true);
        content.addView(button("Changer la langue", false, () -> selectLanguage(this::showWelcome)),
                new LinearLayout.LayoutParams(-1, dp(48)));
        space(content, 18);
        content.addView(text("Votre prochain emploi peut commencer ici.", 27, ink, true));
        space(content, 12);
        content.addView(text("Trouvez des opportunités près de chez vous, consultez les sources officielles et préparez votre profil candidat. Les employeurs peuvent aussi préparer leurs offres.", 16, muted, false));
        space(content, 20);
        content.addView(button("Se connecter", true, () -> openAccountFromWelcome(false)),
                new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 8);
        content.addView(button("Créer un compte", false, () -> openAccountFromWelcome(true)),
                new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 18);
        content.addView(text("Ou découvrir TUN-JOBS sans compte", 13, muted, true));
        space(content, 12);
        content.addView(button("Je cherche un emploi", false, () -> {
            getSharedPreferences("tun_jobs", MODE_PRIVATE).edit().putBoolean("welcome_done", true).apply();
            employerMode = false;
            showHome();
        }), new LinearLayout.LayoutParams(-1, dp(60)));
        space(content, 12);
        content.addView(button("Je propose un emploi", false, () -> {
            getSharedPreferences("tun_jobs", MODE_PRIVATE).edit().putBoolean("welcome_done", true).apply();
            employerMode = true;
            showHome();
        }), new LinearLayout.LayoutParams(-1, dp(60)));
        space(content, 22);
        content.addView(text("TUN-JOBS — Une plateforme tunisienne pour rapprocher candidats et employeurs.", 13, muted, false));
    }

    private void showProfile() {
        prepareScreen(true, this::showProfile);
        base("Votre carte candidat", "Un profil clair pour présenter votre parcours professionnel.", TAB_PROFILE, true);
        profileVisible = true;

        android.content.SharedPreferences p = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE);
        content.addView(text("Aperçu de votre profil", 20, ink, true));
        LinearLayout preview = column();
        preview.setPadding(dp(10), dp(10), dp(10), dp(16));
        preview.setBackground(shape(Color.rgb(25, 27, 33), 24));
        FrameLayout hero = new FrameLayout(this);
        ImageView previewPhoto = new ImageView(this);
        previewPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewPhoto.setBackground(shape(Color.rgb(65, 80, 94), 18));
        Uri savedPhoto = savedProfileFileUri(p, "photo_file", "photo_uri", "profile-photo");
        if (savedPhoto != null) {
            previewPhoto.setImageURI(savedPhoto);
        } else {
            previewPhoto.setImageResource(android.R.drawable.ic_menu_camera);
            previewPhoto.setColorFilter(Color.WHITE);
        }
        hero.addView(previewPhoto, new FrameLayout.LayoutParams(-1, dp(224)));
        LinearLayout identity = column();
        identity.setPadding(dp(16), dp(30), dp(16), dp(14));
        identity.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x00000000, 0xE8000000}));
        identity.addView(text(p.getString("name", "Votre nom"), 25, Color.WHITE, true));
        identity.addView(text(p.getString("experience", "Métier recherché / expérience"), 15, Color.WHITE, true));
        FrameLayout.LayoutParams identityParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        hero.addView(identity, identityParams);
        preview.addView(hero, new LinearLayout.LayoutParams(-1, dp(224)));
        space(preview, 12);
        preview.addView(text("📍  " + p.getString("city", "Kasserine") + " • Tunisie", 14, Color.WHITE, true));
        space(preview, 7);
        String educationPreview = p.getString("education", "");
        if (!educationPreview.trim().isEmpty()) {
            preview.addView(text("🎓  " + educationPreview, 13, Color.rgb(218, 222, 230), false));
            space(preview, 6);
        }
        String skillsPreview = p.getString("skills", "");
        if (!skillsPreview.trim().isEmpty()) {
            preview.addView(text("COMPÉTENCES  •  " + skillsPreview, 13, Color.rgb(218, 222, 230), false));
            space(preview, 6);
        } else {
            preview.addView(text("Ajoutez vos compétences pour compléter votre carte.", 13, muted, false));
        }
        String availabilityPreview = p.getString("availability", "");
        if (!availabilityPreview.trim().isEmpty()) {
            space(preview, 8);
            preview.addView(chip("●  " + availabilityPreview, true));
        }
        space(preview, 10);
        String syncState = p.getString("sync_status", "local");
        String syncLabel = "online".equals(syncState) ? "✓ SYNCHRONISÉ EN LIGNE"
                : ("failed".equals(syncState) ? "⚠ ÉCHEC DE SYNCHRONISATION"
                : (p.getBoolean("profile_saved", false) ? "● ENREGISTRÉ LOCALEMENT • EN ATTENTE" : "APERÇU PRIVÉ • À COMPLÉTER"));
        preview.addView(text(syncLabel, 11, "online".equals(syncState) ? Color.rgb(100, 220, 150) : Color.rgb(255, 205, 92), true));
        space(preview, 8);
        preview.addView(text("E-mail et CV restent masqués dans cet aperçu public.", 12, muted, false));
        content.addView(preview);
        space(content, 18);
        content.addView(text("Modifier mes informations", 20, gold, true));
        space(content, 8);
        ImageView photo = new ImageView(this);
        profilePhotoView = photo;
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackground(shape(panel, 18));
        Uri photoUri = savedProfileFileUri(p, "photo_file", "photo_uri", "profile-photo");
        if (photoUri != null) {
            photo.setImageURI(photoUri);
        } else {
            photo.setImageResource(android.R.drawable.ic_menu_camera);
            photo.setColorFilter(muted);
            photo.setContentDescription("Photo de profil non sélectionnée");
        }
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        photoParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(photo, photoParams);
        space(content, 8);
        content.addView(button("Ajouter / modifier ma photo", false, () -> pickProfileFile(PICK_PROFILE_PHOTO, "image/*")),
                new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 14);

        EditText name = profileField("Nom et prénom", p.getString("name", ""));
        EditText phone = profileField("Téléphone", p.getString("phone", ""));
        EditText email = profileField("Adresse e-mail", p.getString("email", ""));
        EditText city = profileField("Ville / gouvernorat", p.getString("city", "Kasserine"));
        EditText experience = profileField("Expérience et métier recherché", p.getString("experience", ""));
        EditText education = profileField("Formation / diplôme", p.getString("education", ""));
        EditText skills = profileField("Compétences", p.getString("skills", ""));
        EditText availability = profileField("Disponibilité", p.getString("availability", ""));
        EditText[] fields = {name, phone, email, city, experience, education, skills, availability};
        String[] labels = {"Nom et prénom *", "Téléphone *", "E-mail", "Ville / gouvernorat", "Expérience / métier recherché", "Formation / diplôme", "Compétences", "Disponibilité"};
        for (int i=0;i<fields.length;i++) {
            content.addView(text(labels[i], 14, muted, true));
            space(content, 4);
            content.addView(fields[i], new LinearLayout.LayoutParams(-1, dp(64)));
            space(content, 10);
        }

        Uri cvUri = savedProfileFileUri(p, "cv_file", "cv_uri", "profile-cv");
        boolean cvInCloud = !p.getString("cv_remote_path", "").isEmpty();
        profileCvLabel = text(cvUri != null ? "CV enregistré sur cet appareil"
                : (cvInCloud ? "CV synchronisé dans votre espace privé" : "Aucun CV sélectionné"), 14, muted, false);
        content.addView(profileCvLabel);
        space(content, 6);
        content.addView(button("Joindre mon CV (PDF ou document)", false, () -> pickProfileFile(PICK_PROFILE_CV, "application/pdf")),
                new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 14);
        content.addView(button("Enregistrer mon profil sur ce téléphone", true, () -> {
            if (name.getText().toString().trim().isEmpty() || phone.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Le nom et le téléphone sont obligatoires.", Toast.LENGTH_LONG).show();
                return;
            }
            android.content.SharedPreferences.Editor e = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit();
            String[] keys = {"name","phone","email","city","experience","education","skills","availability"};
            for (int i=0;i<fields.length;i++) e.putString(keys[i], fields[i].getText().toString().trim());
            e.putBoolean("profile_saved", true);
            e.putString("sync_status", "pending");
            e.putLong("profile_updated_at", System.currentTimeMillis());
            boolean stored = e.commit();
            if (!stored) {
                Toast.makeText(this, "Erreur : le profil n'a pas pu être enregistré.", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Profil enregistré localement.", Toast.LENGTH_SHORT).show();
            syncCandidateProfile(
                    fields[0].getText().toString().trim(),
                    fields[1].getText().toString().trim(),
                    fields[2].getText().toString().trim(),
                    fields[3].getText().toString().trim(),
                    fields[4].getText().toString().trim(),
                    fields[5].getText().toString().trim(),
                    fields[6].getText().toString().trim(),
                    fields[7].getText().toString().trim());
        }), new LinearLayout.LayoutParams(-1, dp(60)));
        space(content, 8);
        content.addView(text("État du profil : " + ("online".equals(p.getString("sync_status", "local")) ? "synchronisé en ligne" : "enregistré localement, synchronisation en attente").toUpperCase(Locale.ROOT)
                + ". La photo est publiée dans votre profil ; le CV est synchronisé dans un espace privé lorsque vous êtes connecté.", 13, muted, false));

        space(content, 22);
        content.addView(text("Mes offres sauvegardées", 20, ink, true));
        Set<String> saved = new HashSet<String>(getSharedPreferences("gass_jobs_saved", MODE_PRIVATE)
                .getStringSet("keys", new HashSet<String>()));
        int savedCount = 0;
        for (Job job : demoJobs) {
            if (saved.contains(job.key())) {
                infoCard(job.title, job.company + " • " + job.location + " (" + job.governorate + ")", "Voir l'offre", () ->
                        showJob(job.title, job.company, job.location, job.description));
                savedCount++;
            }
        }
        if (savedCount == 0) content.addView(text("Aucune offre sauvegardée pour le moment.", 14, muted, false));
    }

    private void showOfficialJob(Job job) {
        prepareScreen(false, () -> showOfficialJob(job));
        base(job.title, "ANETI • " + job.location, TAB_OFFERS, true);
        content.addView(chip("✓ OFFRE PUBLIÉE PAR ANETI", true));
        space(content, 10);
        content.addView(text("Date de publication : " + job.publishedDate, 14, gold, true));
        space(content, 10);
        content.addView(text(job.description, 16, ink, false));
        space(content, 14);
        content.addView(text("Source : ANETI International. La date limite n'était pas indiquée dans la fiche consultée. Vérifiez que l'offre est toujours ouverte avant de préparer votre candidature.", 13, muted, false));
        space(content, 14);
        content.addView(button("Ouvrir l'annonce originale ANETI", true, () -> openExternal(job.sourceUrl)));
        space(content, 10);
        content.addView(button("Voir toutes les offres ANETI International", false,
                () -> openExternal(ANETI_INTERNATIONAL_OFFERS)));
        space(content, 10);
        content.addView(button("Voir les offres ANETI en Tunisie", false,
                () -> openExternal(ANETI_TUNISIA_OFFERS)));
        space(content, 10);
        content.addView(button("Retour aux cartes", false, this::showHome));
    }

    private void showJob(String title, String company, String location, String description) {
        prepareScreen(false, () -> showJob(title, company, location, description));
        base(title, company + " • " + location, TAB_OFFERS, true);
        content.addView(text(description, 16, ink, false));
        space(content, 18);
        content.addView(button("Postuler (bientôt)", true, this::showNotice));
        space(content, 10);
        content.addView(button("Retour aux offres", false, this::showOffers));
    }

    private EditText draftField(String hint, boolean multiline) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(ink);
        field.setHintTextColor(muted);
        field.setSingleLine(!multiline);
        if (multiline) {
            field.setMinLines(4);
            field.setGravity(Gravity.TOP);
        } else {
            field.setGravity(Gravity.CENTER_VERTICAL);
        }
        field.setPadding(dp(16), dp(12), dp(16), dp(12));
        field.setBackground(shape(panel, 14));
        return field;
    }

    private void showEmployerDraft() {
        prepareScreen(false, this::showEmployerDraft);
        base("Préparer une offre", "Brouillon privé — non publié.", TAB_EMPLOYERS, true);
        content.addView(text("Titre du poste *", 13, muted, true));
        EditText title = draftField("Ex. Technicien de maintenance", false);
        content.addView(title, new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 10);
        content.addView(text("Entreprise *", 13, muted, true));
        EditText company = draftField("Nom de l'entreprise", false);
        content.addView(company, new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 10);
        Spinner region = filterSpinner(new String[] {
                "Kasserine", "Tunis", "Ariana", "Ben Arous", "Manouba", "Nabeul",
                "Zaghouan", "Bizerte", "Béja", "Jendouba", "Le Kef", "Siliana",
                "Kairouan", "Sidi Bouzid", "Sousse", "Monastir", "Mahdia", "Sfax",
                "Gafsa", "Tozeur", "Kébili", "Gabès", "Médenine", "Tataouine"
        });
        addSpinner("Gouvernorat *", region);
        Spinner sector = filterSpinner(new String[] {
                "Commerce", "Agriculture", "Bâtiment", "Informatique", "Santé", "Transport", "Autre"
        });
        addSpinner("Secteur", sector);
        Spinner contract = filterSpinner(new String[] {"CDI", "CDD", "Temps plein", "Temps partiel", "Saisonnier", "Stage"});
        addSpinner("Type de contrat", contract);
        content.addView(text("Salaire (facultatif)", 13, muted, true));
        EditText salary = draftField("Ex. 900–1200 TND / mois", false);
        content.addView(salary, new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 10);
        content.addView(text("Description et compétences *", 13, muted, true));
        EditText description = draftField("Missions, expérience, compétences, horaires…", true);
        content.addView(description, new LinearLayout.LayoutParams(-1, dp(140)));
        space(content, 14);
        content.addView(text("Ne saisissez pas de données personnelles de candidats ici. Le brouillon est enregistré localement sur ce téléphone. La publication, la vérification de l'employeur et les candidatures seront activées après mise en place d'un backend sécurisé.", 12, muted, false));
        space(content, 14);
        content.addView(button("Enregistrer le brouillon sur ce téléphone", true, () -> {
            String jobTitle = title.getText().toString().trim();
            String employer = company.getText().toString().trim();
            String details = description.getText().toString().trim();
            if (jobTitle.isEmpty() || employer.isEmpty() || details.isEmpty()) {
                Toast.makeText(this, "Complétez les champs obligatoires (*).", Toast.LENGTH_LONG).show();
                return;
            }
            getSharedPreferences("gass_jobs_drafts", MODE_PRIVATE).edit()
                    .putString("title", jobTitle)
                    .putString("company", employer)
                    .putString("governorate", selected(region))
                    .putString("sector", selected(sector))
                    .putString("contract", selected(contract))
                    .putString("salary", salary.getText().toString().trim())
                    .putString("description", details)
                    .putLong("saved_at", System.currentTimeMillis())
                    .apply();
            Toast.makeText(this, "Brouillon enregistré sur cet appareil. Rien n'a été publié.", Toast.LENGTH_LONG).show();
        }));
        space(content, 10);
        content.addView(button("Retour", false, this::showEmployerSpace));
        android.content.SharedPreferences saved = getSharedPreferences("gass_jobs_drafts", MODE_PRIVATE);
        if (saved.contains("title")) {
            space(content, 18);
            content.addView(text("Dernier brouillon enregistré", 17, ink, true));
            space(content, 5);
            content.addView(text(saved.getString("title", "") + " — " + saved.getString("company", "") + " (" + saved.getString("governorate", "") + ")", 14, muted, false));
            space(content, 8);
            content.addView(button("Charger ce brouillon", false, () -> {
                title.setText(saved.getString("title", ""));
                company.setText(saved.getString("company", ""));
                salary.setText(saved.getString("salary", ""));
                description.setText(saved.getString("description", ""));
                setSpinnerValue(region, saved.getString("governorate", "Kasserine"));
                setSpinnerValue(sector, saved.getString("sector", "Commerce"));
                setSpinnerValue(contract, saved.getString("contract", "CDI"));
            }));
            space(content, 8);
            content.addView(button("Supprimer le brouillon", false, () -> {
                saved.edit().clear().apply();
                Toast.makeText(this, "Brouillon supprimé de cet appareil.", Toast.LENGTH_SHORT).show();
                showEmployerDraft();
            }));
        }
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) spinner.setSelection(position);
    }

    private void showNotice() {
        prepareScreen(false, this::showNotice);
        base("Fonction en préparation", "Cette première version est une base de démonstration.");
        content.addView(text("Les candidatures, comptes et publications seront ajoutés après la mise en place du backend sécurisé.", 15, muted, false));
        space(content, 14);
        content.addView(button("Retour", true, this::showHome));
    }
}
