package com.gass.jobs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.InputType;
import android.util.Patterns;
import android.view.HapticFeedbackConstants;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.Switch;
import java.util.Random;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import org.json.JSONObject;

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
    private int swipeIndex = 0;
    private int candidateSwipeIndex = 0;
    private final ArrayList<Job> visibleJobs = new ArrayList<Job>();

    private static final int TAB_HOME = 0, TAB_OFFERS = 1, TAB_PROFILE = 2, TAB_EMPLOYERS = 3, TAB_MORE = 4;
    private FrameLayout deckHolder;
    private LinearLayout actionBar;
    private TextView likeStamp, nopeStamp;
    private boolean animating = false;
    private ScrollView mainScroll;
    private boolean chromeless = false;
    private boolean tipDismissed = false;
    private long avatarSalt = System.nanoTime();
    private boolean authSignupMode = false;
    private static final String SUPABASE_URL = "https://rwmldlqjwgtjckspqkfl.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_aYD3mQuWRYLMbqjLCDoBLA_RVdRxu4_";
    private static final String PREFS_AUTH = "tun_jobs_auth";

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
        final String name, profession, city, experience, education, skills, availability, photoAsset;
        Candidate(String name, String profession, String city, String experience, String education, String skills, String availability) {
            this(name, profession, city, experience, education, skills, availability, "");
        }
        Candidate(String name, String profession, String city, String experience, String education, String skills, String availability, String photoAsset) {
            this.name = name;
            this.profession = profession;
            this.city = city;
            this.experience = experience;
            this.education = education;
            this.skills = skills;
            this.availability = availability;
            this.photoAsset = photoAsset;
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

        // Eight fictional demo cards using the supplied portraits.
        add(new Candidate("Profil démo 01", "Assistante administrative", "Kasserine", "2 ans", "Baccalauréat", "Bureautique • Accueil • Organisation", "Disponible immédiatement", "demo_candidates/IMG_20261111_091036.jpg"));
        add(new Candidate("Profil démo 02", "Conseillère commerciale", "Sbeitla", "1 an", "Formation commerciale", "Vente • Communication • Client", "Temps plein", "demo_candidates/IMG_20261111_091042.jpg"));
        add(new Candidate("Profil démo 03", "Technicienne informatique", "Kasserine", "2 ans", "Technicienne supérieure", "Informatique • Support • Réseaux", "Disponible immédiatement", "demo_candidates/IMG_20261111_091049.jpg"));
        add(new Candidate("Profil démo 04", "Assistante comptable", "Kairouan", "3 ans", "Licence gestion", "Excel • Comptabilité • Facturation", "Temps plein", "demo_candidates/IMG_20261111_091056.jpg"));
        add(new Candidate("Profil démo 05", "Chargée d'accueil", "Fériana", "1 an", "Baccalauréat", "Accueil • Service • Organisation", "Temps partiel", "demo_candidates/IMG_20261111_091104.jpg"));
        add(new Candidate("Profil démo 06", "Technicienne de laboratoire", "Tunis", "2 ans", "Formation spécialisée", "Analyse • Hygiène • Précision", "Disponible sous 2 semaines", "demo_candidates/IMG_20261111_091112.jpg"));
        add(new Candidate("Profil démo 07", "Secrétaire médicale", "Kasserine", "1 an", "Formation secrétariat", "Accueil • Dossiers • Bureautique", "Disponible immédiatement", "demo_candidates/IMG_20261111_091122.jpg"));
        add(new Candidate("Profil démo 08", "Assistante ressources humaines", "Sousse", "2 ans", "Licence RH", "Recrutement • Organisation • Communication", "Temps plein", "demo_candidates/IMG_20261111_091128.jpg"));
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
        if (hasAuthSession()) showHome();
        else if (getSharedPreferences("tun_jobs", MODE_PRIVATE).getBoolean("welcome_done", false)) showAuth();
        else showWelcome();
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
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15 * textScale());
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
        v.setText(value);
        v.setTextSize(size * textScale());
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
        if (pendingRootScreen) {
            screenHistory.clear();
        } else if (!restoringScreen && currentScreenAction != null) {
            screenHistory.add(currentScreenAction);
        }
        if (pendingScreenAction != null) currentScreenAction = pendingScreenAction;
        pendingScreenAction = null;
        pendingRootScreen = false;

        root = column();
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
        if (!chromeless) {
        LinearLayout top = row();
        top.setPadding(dp(8), dp(6), dp(8), dp(6));
        top.setBackgroundColor(Color.rgb(8, 9, 12));
        TextView menuIcon = topIcon("☰", 26, this::showMenu);
        menuIcon.setContentDescription("Menu");
        explain(menuIcon, "Menu", "Ouvre le menu : votre profil, les paramètres, les conseils, les sites officiels de l'emploi et les bureaux régionaux.");
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
        explain(searchIcon, "Recherche", "Ouvre la recherche d'offres par métier, gouvernorat, secteur ou type de contrat.");
        top.addView(searchIcon, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView bellIcon = topIcon("🔔", 20, () ->
                Toast.makeText(this, "Aucune nouvelle notification.", Toast.LENGTH_SHORT).show());
        bellIcon.setContentDescription("Notifications");
        explain(bellIcon, "Notifications", "Les alertes (nouvelles offres, réponses des employeurs) arriveront ici quand les comptes en ligne seront disponibles.");
        top.addView(bellIcon, new LinearLayout.LayoutParams(dp(44), dp(48)));
        root.addView(top);
        }
        chromeless = false;

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
        mainScroll = null;
        if (scrollable) {
            ScrollView scroll = new ScrollView(this);
            mainScroll = scroll;
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
        String[] navHelp = {
            "L'accueil : les cartes d'offres à faire glisser (ou de profils si vous recrutez).",
            "Recherche d'offres avec filtres : métier, gouvernorat, secteur et contrat.",
            "Votre profil candidat : photo, informations, CV et offres sauvegardées. Tout reste sur ce téléphone.",
            "L'espace recruteur : identifiez-vous (seul ou société), préparez vos offres et découvrez des profils.",
            "Menu, paramètres, conseils et sites officiels de l'emploi."
        };
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
            explain(item, labels[i], navHelp[i]);
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
        addSpinnerTo(content, label, spinner);
    }

    private void addSpinnerTo(LinearLayout parent, String label, Spinner spinner) {
        parent.addView(text(label, 13, muted, true));
        space(parent, 4);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(shape(panel, 14));
        spinner.setBackground(null);
        box.addView(spinner, new FrameLayout.LayoutParams(-1, -1));
        TextView arrow = text("▾", 18, muted, true);
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL);
        arrowParams.rightMargin = dp(14);
        box.addView(arrow, arrowParams);
        parent.addView(box, new LinearLayout.LayoutParams(-1, dp(48)));
        space(parent, 10);
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
                                 String line1, String line2, String desc, String[] chips, String counter, Drawable art) {
        FrameLayout card = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        bg.setCornerRadius(dp(28));
        card.setBackground(bg);
        card.setClipToOutline(true);

        if (art != null) {
            ImageView portrait = new ImageView(this);
            portrait.setImageDrawable(art);
            portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(portrait, new FrameLayout.LayoutParams(-1, -1));
        } else {
            TextView avatar = text(glyph, 52, Color.WHITE, true);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(circle(Color.argb(46, 255, 255, 255)));
            FrameLayout.LayoutParams avatarParams = new FrameLayout.LayoutParams(dp(112), dp(112), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            avatarParams.topMargin = dp(64);
            card.addView(avatar, avatarParams);
        }

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
        return deckCard(palette(job.sector), badge, job.location, sectorGlyph(job.sector),
                job.title, "🏢  " + job.company, "📍  " + job.location + dateLine,
                job.description, new String[] {job.sector, job.contract}, (index + 1) + "/" + total, null);
    }

    private Drawable loadDemoPortrait(String assetPath) {
        if (assetPath == null || assetPath.isEmpty()) return null;
        try (java.io.InputStream input = getAssets().open(assetPath)) {
            return Drawable.createFromStream(input, null);
        } catch (Exception e) {
            android.util.Log.w("GASS-JOBS", "Demo portrait unavailable: " + assetPath, e);
            return null;
        }
    }

    private View candidateCard(Candidate c, int index, int total) {
        String[] skills = c.skills.split(" • ");
        String[] chips = new String[Math.min(3, skills.length)];
        for (int i = 0; i < chips.length; i++) chips[i] = skills[i];
        Drawable portrait = c.photoAsset.isEmpty()
                ? new AvatarDrawable(avatarSeed(c)) : loadDemoPortrait(c.photoAsset);
        if (portrait == null) portrait = new AvatarDrawable(avatarSeed(c));
        return deckCard(palette(c.name), "✓ Profil de démonstration", c.city,
                c.name.substring(0, 1).toUpperCase(Locale.ROOT),
                c.name, "💼  " + c.profession, "🎓  " + c.education + " • " + c.experience,
                "● " + c.availability + " — profil fictif, aucun contact réel.", chips, (index + 1) + "/" + total,
                portrait);
    }

    private void fly(View card, int direction, Runnable after) {
        if (animating) return;
        animating = true;
        if (hapticsEnabled()) card.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        float distance = getResources().getDisplayMetrics().widthPixels * 1.2f;
        card.animate().translationX(direction * distance).rotation(direction * 18f).alpha(0f)
                .setDuration(220).withEndAction(after).start();
    }

    private void attachSwipe(final View card, final TextView like, final TextView nope,
                             final Runnable onLeft, final Runnable onRight, final Runnable onTap,
                             final Runnable onLong) {
        final float[] startX = {0f};
        final boolean[] moved = {false};
        final boolean[] longFired = {false};
        final Runnable longPress = () -> {
            if (moved[0] || animating || !helpEnabled()) return;
            longFired[0] = true;
            card.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            onLong.run();
        };
        card.setOnTouchListener((v, e) -> {
            if (animating) return true;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                startX[0] = e.getRawX();
                moved[0] = false;
                longFired[0] = false;
                v.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            if (longFired[0]) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.animate().translationX(0f).rotation(0f).setDuration(120).start();
                }
                return true;
            }
            float dx = e.getRawX() - startX[0];
            if (action == MotionEvent.ACTION_MOVE) {
                if (Math.abs(dx) > dp(8)) {
                    moved[0] = true;
                    v.removeCallbacks(longPress);
                }
                v.setTranslationX(dx);
                v.setRotation(dx / Math.max(1, v.getWidth()) * 14f);
                float progress = Math.min(1f, Math.abs(dx) / (Math.max(1, v.getWidth()) * 0.3f));
                like.setAlpha(dx > 0 ? progress : 0f);
                nope.setAlpha(dx < 0 ? progress : 0f);
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                v.removeCallbacks(longPress);
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
                v.removeCallbacks(longPress);
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
        explain(b, label, actionHelp(label));
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
                : "Vous avez parcouru les offres ANETI et les profils fictifs de démonstration.", 18, ink, true);
        msg.setGravity(Gravity.CENTER);
        box.addView(msg);
        space(box, 18);
        box.addView(button("Recommencer", true, () -> {
            if (employerMode) candidateSwipeIndex = 0; else swipeIndex = 0;
            avatarSalt = System.nanoTime();
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
                        + "\n\nProfil fictif de démonstration : l'avatar est généré au hasard et ne représente pas une vraie personne. Aucun contact réel n'est associé.")
                .setPositiveButton("Fermer", (dialog, which) -> dialog.dismiss()).show();
    }

    private void renderDeck() {
        animating = false;
        deckHolder.removeAllViews();
        actionBar.removeAllViews();

        // Jobseekers see official ANETI offers first, followed by clearly labelled
        // fictional candidate profiles using the eight supplied portrait photos.
        // Recruiters retain their independent full candidate deck.
        final boolean recruiterDeck = employerMode;
        final int total = recruiterDeck ? demoCandidates.size() : anetiJobs.size() + demoCandidates.size();
        final int index = recruiterDeck ? candidateSwipeIndex : swipeIndex;
        if (index >= total) {
            deckHolder.addView(emptyDeck(recruiterDeck), new FrameLayout.LayoutParams(-1, -1));
            return;
        }

        final boolean demoProfile = recruiterDeck || index >= anetiJobs.size();
        final int candidateIndex = recruiterDeck ? index : index - anetiJobs.size();
        final Job job = demoProfile ? null : anetiJobs.get(index);
        final Candidate person = demoProfile ? demoCandidates.get(candidateIndex) : null;

        if (index + 1 < total) {
            View behind = new View(this);
            behind.setBackground(shape(Color.rgb(26, 28, 34), 28));
            FrameLayout.LayoutParams behindParams = new FrameLayout.LayoutParams(-1, -1);
            behindParams.setMargins(dp(16), dp(16), dp(16), 0);
            deckHolder.addView(behind, behindParams);
        }

        final View card = demoProfile
                ? candidateCard(person, candidateIndex, demoCandidates.size())
                : jobCard(job, index, total);
        final TextView like = likeStamp;
        final TextView nope = nopeStamp;
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(-1, -1);
        cardParams.bottomMargin = dp(14);
        deckHolder.addView(card, cardParams);

        final Runnable nextCard = () -> {
            if (recruiterDeck) candidateSwipeIndex++; else swipeIndex++;
            renderDeck();
        };
        final Runnable saveThenNext = () -> {
            if (demoProfile) saveCandidate(person); else saveJob(job);
            nextCard.run();
        };
        final Runnable details = () -> {
            if (demoProfile) showCandidateDialog(person);
            else if (job.isOfficial()) showOfficialJob(job);
            else showJob(job.title, job.company, job.location, job.description);
        };
        final Runnable cardHelp = () -> showHelp(demoProfile ? "Profil candidat fictif" : "Offre ANETI",
                demoProfile
                        ? "Cette carte présente un profil fictif avec une photo illustrative. Les informations et le contact ne représentent pas une vraie personne. Glissez à droite pour présélectionner, à gauche pour passer."
                        : "Glissez vers la droite pour sauvegarder l'offre dans votre profil, vers la gauche pour la passer. Touchez la carte pour ouvrir le détail. Les cartes ANETI viennent du site officiel : vérifiez l'annonce originale avant de postuler.");
        attachSwipe(card, like, nope, nextCard, saveThenNext, details, cardHelp);

        addAction("↺", "Retour", Color.rgb(52, 54, 62), 50, () -> {
            if (recruiterDeck) candidateSwipeIndex = Math.max(0, candidateSwipeIndex - 1);
            else swipeIndex = Math.max(0, swipeIndex - 1);
            renderDeck();
        });
        addAction("✕", "Ignorer", Color.rgb(229, 57, 53), 58, () -> {
            nope.setAlpha(1f);
            fly(card, -1, nextCard);
        });
        addAction("★", demoProfile ? "Présélectionner" : "Sauvegarder", Color.rgb(245, 178, 35), 58, () -> {
            like.setAlpha(1f);
            fly(card, 1, saveThenNext);
        });
        addAction("♥", demoProfile ? "Contact démo" : "Contacter", Color.rgb(30, 170, 95), 58, () -> {
            if (demoProfile) {
                Toast.makeText(this, "Profil fictif : aucun contact réel n'est associé.", Toast.LENGTH_LONG).show();
            } else if (job.isOfficial()) {
                openExternal(job.sourceUrl);
            } else {
                showNotice();
            }
        });
        addAction("➤", "Partager", Color.rgb(35, 110, 230), 50, () -> {
            String message = demoProfile
                    ? "Découvrez TUN-JOBS — profil fictif de démonstration, sans contact réel."
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
        switcher.addView(explain(segment("♙  Cherche un emploi", !employerMode, () -> { employerMode = false; showHome(); }),
                "Cherche un emploi", "Affiche les offres d'emploi sous forme de cartes à faire glisser."),
                new LinearLayout.LayoutParams(0, dp(44), 1));
        switcher.addView(explain(segment("▣  Propose un emploi", employerMode, this::chooseEmployerMode),
                "Propose un emploi", "Pour recruter : vous indiquez d'abord si vous recrutez seul(e) ou pour une société, puis vous découvrez des profils et préparez vos offres."),
                new LinearLayout.LayoutParams(0, dp(44), 1));
        content.addView(switcher, new LinearLayout.LayoutParams(-1, -2));
        space(content, 8);
        if (!employerMode) {
            content.addView(explain(button("🇹🇳  Offres ANETI en Tunisie", false,
                    () -> openExternal(ANETI_TUNISIA_OFFERS)),
                    "Offres ANETI en Tunisie",
                    "Ouvre le site officiel de l'Agence nationale pour l'emploi et le travail indépendant, avec les offres en Tunisie."),
                    new LinearLayout.LayoutParams(-1, dp(46)));
            space(content, 6);
            content.addView(text("Cartes suivantes : offres ANETI International publiées par la source officielle. Vérifiez la disponibilité et les délais sur chaque annonce.", 11, muted, false));
        } else {
            content.addView(text("Profils candidats de démonstration — aucun profil réel n'est publié.", 11, muted, false));
        }
        addTipRow();
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
        chooseEmployerMode();
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
            item.addView(text(job.company + " • " + job.location, 14, muted, false));
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
        if (!hasEmployerProfile()) {
            showEmployerType(this::showEmployerSpace);
            return;
        }
        prepareScreen(true, this::showEmployerSpace);
        base("Espace recruteur", "Préparez vos offres et découvrez des profils.", TAB_EMPLOYERS, true);
        addEmployerSummary();
        content.addView(text("Découvrez des profils candidats sous forme de cartes, puis préparez vos offres d'emploi.", 14, muted, false));
        space(content, 14);
        content.addView(explain(button("♥  Découvrir les candidats", true, this::showCandidateDeck),
                "Découvrir les candidats", "Affiche des profils candidats sous forme de cartes. Dans cette version, les profils sont fictifs."),
                new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 6);
        content.addView(text("Les profils affichés dans cette démonstration sont fictifs. Aucun candidat réel n'est publié ni contacté.", 12, muted, false));
        space(content, 18);
        content.addView(explain(button("＋  Créer une offre", true, this::showEmployerDraft),
                "Créer une offre", "Prépare une offre d'emploi. Le brouillon reste sur ce téléphone : rien n'est publié tant que la publication sécurisée n'existe pas."),
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
        content.addView(button("⌂   Emplois", false, this::showHome), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("♙   Mon profil", false, this::showProfile), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("⚙   Paramètres", false, this::showSettings), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("💡   Conseils et aide", false, this::showTips), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("ⓘ   À propos", false, this::showAboutMe), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("Sites officiels de l'emploi", false, this::showOfficialSources), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("Bureaux régionaux", false, this::showEmploymentOffices), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 8);
        content.addView(button("⇥   Se déconnecter", false, () -> confirmAction("Se déconnecter ?", "Votre session sera fermée sur cet appareil. Vos données locales seront conservées.", "Déconnexion", this::logout)), new LinearLayout.LayoutParams(-1, dp(58)));
    }

    private void showSettings() {
        prepareScreen(false, this::showSettings);
        base("Paramètres", "Personnalisez TUN-JOBS sur cet appareil.");

        sectionTitle("Apparence");
        choiceRow("Taille du texte", "Agrandissez le texte si vous lisez difficilement.",
                "text_size", new String[] {"Petit", "Normal", "Grand"}, 1);

        sectionTitle("Cartes et aide");
        switchRow("Aide par appui long", "Maintenez un bouton ou une carte pour afficher son explication.", "help_long_press", true);
        switchRow("Astuces sur l'accueil", "Affiche un petit conseil au-dessus des cartes.", "tips", true);
        switchRow("Vibration légère", "Retour tactile lors des gestes sur les cartes.", "haptics", true);

        sectionTitle("Ma région");
        Spinner gov = filterSpinner(GOVERNORATES);
        addSpinner("Gouvernorat par défaut", gov);
        setSpinnerValue(gov, defaultGovernorate());
        gov.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = String.valueOf(parent.getItemAtPosition(position));
                if (!value.equals(defaultGovernorate())) {
                    settings().edit().putString("default_gov", value).apply();
                    Toast.makeText(MainActivity.this, "Région par défaut : " + value, Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        content.addView(text("Utilisée pour pré-remplir votre profil et vos offres.", 12, muted, false));
        space(content, 14);

        sectionTitle("Langue");
        content.addView(text("L'interface est en français. L'arabe et l'anglais seront ajoutés progressivement.", 13, muted, false));
        space(content, 14);

        sectionTitle("Mon compte");
        content.addView(text("Connecté avec : " + authPrefs().getString("email", "Compte TUN-JOBS"), 13, muted, false));
        space(content, 8);
        content.addView(button("⇥   Se déconnecter", false, () -> confirmAction("Se déconnecter ?", "Votre session sera fermée. Vos données locales restent sur ce téléphone.", "Déconnexion", this::logout)), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 8);
        content.addView(button("♙   Mon profil candidat", false, this::showProfile), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 8);
        content.addView(button(hasEmployerProfile() ? "▣   Mon profil recruteur (seul / société)" : "▣   Devenir recruteur", false,
                () -> showEmployerType(this::showSettings)), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 14);

        sectionTitle("Aide");
        content.addView(button("▶   Revoir le tutoriel", false, () -> showWelcomeStep(0)), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 8);
        content.addView(button("💡   Conseils et aide", false, this::showTips), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 14);

        sectionTitle("Données sur ce téléphone");
        content.addView(text("Votre profil, votre photo, votre CV, vos sauvegardes et vos brouillons sont stockés uniquement sur ce téléphone. Aucun compte en ligne pour l'instant.", 13, muted, false));
        space(content, 10);
        content.addView(button("Effacer mes offres et profils sauvegardés", false, () ->
                confirmAction("Effacer les sauvegardes ?", "Les offres et profils que vous avez sauvegardés seront supprimés de ce téléphone.", "Effacer", () -> {
                    clearSavedItems();
                    Toast.makeText(this, "Sauvegardes effacées.", Toast.LENGTH_SHORT).show();
                })), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 8);
        content.addView(button("Effacer toutes mes données", false, () ->
                confirmAction("Tout effacer ?", "Profil, photo, CV, profil recruteur, brouillons et sauvegardes seront définitivement supprimés de ce téléphone.", "Tout effacer", () -> {
                    clearEverything();
                    Toast.makeText(this, "Toutes vos données ont été effacées.", Toast.LENGTH_LONG).show();
                    showSettings();
                })), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 14);

        sectionTitle("À propos");
        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) { }
        content.addView(text("TUN-JOBS • version " + version + " • par GASS-OS", 13, muted, false));
        space(content, 10);
        content.addView(button("➤   Partager TUN-JOBS", false, () -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, "Découvrez TUN-JOBS, l'application emploi et recrutement en Tunisie.");
            startActivity(Intent.createChooser(send, "Partager"));
        }), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 8);
        content.addView(button("ⓘ   À propos", false, this::showAboutMe), new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 14);
        content.addView(button("Retour aux emplois", true, this::showHome), new LinearLayout.LayoutParams(-1, dp(58)));
    }

    private void showAboutMe() {
        prepareScreen(false, this::showAboutMe);
        base("À propos", "Qui sommes-nous ?");
        LinearLayout banner = column();
        banner.setPadding(dp(20), dp(22), dp(20), dp(22));
        banner.setBackground(shape(Color.rgb(22, 35, 52), 18));
        banner.addView(text("TUN-JOBS", 28, Color.WHITE, true));
        space(banner, 8);
        banner.addView(text("Une plateforme tunisienne pour rapprocher les candidats et les employeurs.", 16, Color.WHITE, false));
        content.addView(banner);
        space(content, 18);
        content.addView(text("Créé par GASS-OS", 20, green, true));
        space(content, 6);
        content.addView(text("Conçu par Aymen Gharsalli. Notre objectif est de faciliter l'accès aux opportunités d'emploi à Kasserine et dans les 24 gouvernorats de Tunisie.", 15, muted, false));
        space(content, 16);
        content.addView(text("Version de démonstration", 17, ink, true));
        space(content, 6);
        content.addView(text("Les annonces visibles actuellement sont des exemples fictifs. Les portails publics s'ouvrent directement sur leurs sites officiels. Les comptes, candidatures et publications en ligne nécessitent encore un service sécurisé.", 14, muted, false));
        space(content, 18);
        content.addView(button("Retour", true, this::showMenu), new LinearLayout.LayoutParams(-1, dp(58)));
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
        field.setHint(hint);
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
        android.content.SharedPreferences.Editor edit = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit();
        if (requestCode == PICK_PROFILE_PHOTO) edit.putString("photo_file", copied).remove("photo_uri");
        if (requestCode == PICK_PROFILE_CV) edit.putString("cv_file", copied).remove("cv_uri");
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
    }

    private void showWelcome() {
        showWelcomeStep(0);
    }

    private void finishWelcome(boolean recruiter) {
        getSharedPreferences("tun_jobs", MODE_PRIVATE).edit().putBoolean("welcome_done", true).apply();
        getSharedPreferences(PREFS_AUTH, MODE_PRIVATE).edit().putBoolean("pending_employer_mode", recruiter).apply();
        showAuth();
    }

    private void showWelcomeStep(final int step) {
        prepareScreen(true, () -> showWelcomeStep(step));
        chromeless = true;
        base(null, null, -1, true);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        final int last = 4;
        String[] icons = {"💼", "👆", "🇹🇳", "💡", "🙋"};
        String[] titles = {
            "Bienvenue sur TUN-JOBS",
            "Swipez vos offres",
            "Des offres officielles",
            "Appui long = aide",
            "Qui êtes-vous ?"
        };
        String[] bodies = {
            "L'emploi et le recrutement, partout en Tunisie. Trouvez des opportunités près de chez vous et préparez votre profil en quelques minutes.",
            "Glissez une carte vers la droite pour la sauvegarder, vers la gauche pour la passer. Touchez-la pour lire tous les détails. Les boutons ronds sous la carte font la même chose.",
            "Les cartes « ANETI » viennent du site officiel de l'Agence nationale pour l'emploi. Ouvrez toujours l'annonce originale et vérifiez la date limite. Les autres cartes sont des exemples de démonstration.",
            "Un bouton vous intrigue ? Maintenez le doigt dessus : une fenêtre explique à quoi il sert. Essayez avec le bouton ci-dessous !",
            "Pour commencer, dites-nous ce que vous faites. Les recruteurs indiquent d'abord s'ils recrutent seuls ou pour une société."
        };

        LinearLayout topRow = row();
        topRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        if (step < last) {
            TextView skip = text("Passer", 14, muted, true);
            skip.setPadding(dp(14), dp(10), dp(14), dp(10));
            skip.setOnClickListener(v -> finishWelcome(false));
            topRow.addView(skip);
        }
        content.addView(topRow, new LinearLayout.LayoutParams(-1, dp(44)));
        space(content, 14);

        TextView icon = text(icons[step], 46, Color.WHITE, false);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(circle(Color.argb(40, 255, 190, 45)));
        content.addView(icon, new LinearLayout.LayoutParams(dp(112), dp(112)));
        space(content, 22);
        TextView title = text(titles[step], 27, ink, true);
        title.setGravity(Gravity.CENTER);
        content.addView(title, new LinearLayout.LayoutParams(-1, -2));
        space(content, 12);
        TextView body = text(bodies[step], 16, muted, false);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(0f, 1.15f);
        content.addView(body, new LinearLayout.LayoutParams(-1, -2));

        if (step == 1) {
            space(content, 18);
            LinearLayout demo = row();
            demo.setGravity(Gravity.CENTER);
            TextView left = text("✕  Passer", 14, Color.WHITE, true);
            left.setBackground(shape(Color.rgb(160, 50, 48), 18));
            left.setPadding(dp(14), dp(8), dp(14), dp(8));
            TextView right = text("★  Sauvegarder", 14, Color.rgb(20, 20, 22), true);
            right.setBackground(shape(gold, 18));
            right.setPadding(dp(14), dp(8), dp(14), dp(8));
            demo.addView(left);
            hspace(demo, 12);
            demo.addView(right);
            content.addView(demo, new LinearLayout.LayoutParams(-2, -2));
        } else if (step == 3) {
            space(content, 18);
            content.addView(explain(button("🧪  Maintenez-moi appuyé", false, () ->
                    Toast.makeText(this, "Appuyez longtemps, sans relâcher.", Toast.LENGTH_SHORT).show()),
                    "Bravo, ça marche !",
                    "Un appui long sur un bouton, une carte ou un onglet affiche une explication comme celle-ci. Vous pouvez le désactiver dans Paramètres."),
                    new LinearLayout.LayoutParams(-1, dp(54)));
        }

        space(content, 24);
        LinearLayout dots = row();
        dots.setGravity(Gravity.CENTER);
        for (int i = 0; i <= last; i++) {
            View dot = new View(this);
            dot.setBackground(shape(i == step ? gold : Color.rgb(60, 63, 72), 4));
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(i == step ? 24 : 8), dp(8));
            dotParams.setMargins(dp(4), 0, dp(4), 0);
            dots.addView(dot, dotParams);
        }
        content.addView(dots, new LinearLayout.LayoutParams(-2, -2));
        space(content, 24);

        if (step < last) {
            LinearLayout navRow = row();
            if (step > 0) {
                navRow.addView(button("Précédent", false, () -> showWelcomeStep(step - 1)), new LinearLayout.LayoutParams(0, dp(54), 1));
                hspace(navRow, 10);
            }
            navRow.addView(button("Suivant", true, () -> showWelcomeStep(step + 1)), new LinearLayout.LayoutParams(0, dp(54), 1));
            content.addView(navRow, new LinearLayout.LayoutParams(-1, -2));
        } else {
            content.addView(explain(button("Je cherche un emploi", true, () -> finishWelcome(false)),
                    "Je cherche un emploi", "Vous accédez aux offres. Vous pourrez compléter votre profil et votre CV plus tard."),
                    new LinearLayout.LayoutParams(-1, dp(58)));
            space(content, 10);
            content.addView(explain(button("Je propose un emploi", false, () -> finishWelcome(true)),
                    "Je propose un emploi", "Vous précisez si vous recrutez seul(e) ou pour une société, puis vous accédez à l'espace recruteur."),
                    new LinearLayout.LayoutParams(-1, dp(58)));
            space(content, 10);
            content.addView(button("Précédent", false, () -> showWelcomeStep(step - 1)), new LinearLayout.LayoutParams(-1, dp(48)));
        }
    }

    private void showProfile() {
        prepareScreen(true, this::showProfile);
        base("Votre carte candidat", "Un profil clair pour présenter votre parcours professionnel.", TAB_PROFILE, true);

        android.content.SharedPreferences p = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE);
        content.addView(text("Aperçu de votre profil", 20, ink, true));
        LinearLayout preview = column();
        preview.setPadding(dp(18), dp(18), dp(18), dp(18));
        preview.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {Color.rgb(57, 45, 25), Color.rgb(15, 17, 23), Color.rgb(8, 10, 14)}));
        ImageView previewPhoto = new ImageView(this);
        previewPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewPhoto.setBackground(shape(Color.rgb(65, 80, 94), 22));
        Uri savedPhoto = savedProfileFileUri(p, "photo_file", "photo_uri", "profile-photo");
        if (savedPhoto != null) {
            previewPhoto.setImageURI(savedPhoto);
        } else {
            previewPhoto.setImageResource(android.R.drawable.ic_menu_camera);
            previewPhoto.setColorFilter(Color.WHITE);
        }
        LinearLayout.LayoutParams previewPhotoParams = new LinearLayout.LayoutParams(dp(104), dp(104));
        previewPhotoParams.gravity = Gravity.CENTER_HORIZONTAL;
        preview.addView(previewPhoto, previewPhotoParams);
        space(preview, 12);
        preview.addView(text(p.getString("name", "Votre nom"), 23, Color.WHITE, true));
        space(preview, 4);
        preview.addView(text(p.getString("experience", "Métier recherché / expérience"), 15, Color.WHITE, true));
        preview.addView(text(p.getString("city", "Kasserine") + " • Tunisie", 13, Color.rgb(225, 235, 242), false));
        space(preview, 8);
        preview.addView(text(p.getString("skills", "Ajoutez vos compétences pour compléter votre carte."), 13, Color.WHITE, false));
        space(preview, 10);
        preview.addView(text(p.getBoolean("profile_saved", false) ? "PROFIL ENREGISTRÉ SUR CET APPAREIL" : "APERÇU PRIVÉ • À COMPLÉTER", 11, Color.rgb(255, 205, 92), true));
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
        profileCvLabel = text(cvUri == null ? "Aucun CV sélectionné" : "CV enregistré sur cet appareil", 14, muted, false);
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
            e.putLong("profile_updated_at", System.currentTimeMillis());
            boolean stored = e.commit();
            Toast.makeText(this, stored
                    ? "Profil et informations enregistrés sur cet appareil."
                    : "Erreur : le profil n'a pas pu être enregistré.", Toast.LENGTH_LONG).show();
        }), new LinearLayout.LayoutParams(-1, dp(60)));
        space(content, 8);
        content.addView(text("Enregistrement local : vos informations sont conservées dans l'application et votre photo / CV sont copiés dans son espace privé. Ils restent disponibles après fermeture et redémarrage de l'application. Rien n'est envoyé ni publié.", 13, muted, false));

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
        if (!employerDisplayName().isEmpty()) company.setText(employerDisplayName());
        content.addView(company, new LinearLayout.LayoutParams(-1, dp(52)));
        space(content, 10);
        Spinner region = filterSpinner(new String[] {
                "Kasserine", "Tunis", "Ariana", "Ben Arous", "Manouba", "Nabeul",
                "Zaghouan", "Bizerte", "Béja", "Jendouba", "Le Kef", "Siliana",
                "Kairouan", "Sidi Bouzid", "Sousse", "Monastir", "Mahdia", "Sfax",
                "Gafsa", "Tozeur", "Kébili", "Gabès", "Médenine", "Tataouine"
        });
        addSpinner("Gouvernorat *", region);
        setSpinnerValue(region, employerGovernorate());
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

    // ================================================================ settings, help, tips
    private static final String PREFS_SETTINGS = "tun_jobs_settings";
    private static final String PREFS_EMPLOYER = "tun_jobs_employer";
    private static final String[] GOVERNORATES = {
        "Kasserine", "Tunis", "Ariana", "Ben Arous", "Manouba", "Nabeul",
        "Zaghouan", "Bizerte", "Béja", "Jendouba", "Le Kef", "Siliana",
        "Kairouan", "Sidi Bouzid", "Sousse", "Monastir", "Mahdia", "Sfax",
        "Gafsa", "Tozeur", "Kébili", "Gabès", "Médenine", "Tataouine"
    };

    private SharedPreferences settings() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
    }

    private float textScale() {
        int size = settings().getInt("text_size", 1);
        return size == 0 ? 0.9f : (size == 2 ? 1.15f : 1f);
    }

    private boolean helpEnabled() {
        return settings().getBoolean("help_long_press", true);
    }

    private boolean hapticsEnabled() {
        return settings().getBoolean("haptics", true);
    }

    private boolean tipsEnabled() {
        return settings().getBoolean("tips", true);
    }

    private String defaultGovernorate() {
        return settings().getString("default_gov", "Kasserine");
    }

    /** Re-renders the current screen in place (no new history entry, scroll position kept). */
    private void refreshScreen() {
        if (currentScreenAction == null) return;
        final ScrollView old = mainScroll;
        final int y = old != null ? old.getScrollY() : 0;
        restoringScreen = true;
        try {
            currentScreenAction.run();
        } finally {
            restoringScreen = false;
        }
        final ScrollView fresh = mainScroll;
        if (fresh != null) fresh.post(() -> fresh.scrollTo(0, y));
    }

    private void confirmAction(String title, String message, String yes, final Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .setPositiveButton(yes, (dialog, which) -> action.run())
                .show();
    }

    private void clearSavedItems() {
        String[] names = {"gass_jobs_saved", "tun_jobs_candidates"};
        for (String name : names) getSharedPreferences(name, MODE_PRIVATE).edit().clear().apply();
    }

    private void clearEverything() {
        clearSavedItems();
        String[] names = {"tun_jobs_profile", "gass_jobs_drafts", PREFS_EMPLOYER};
        for (String name : names) getSharedPreferences(name, MODE_PRIVATE).edit().clear().apply();
        new File(getFilesDir(), "profile-photo").delete();
        new File(getFilesDir(), "profile-cv").delete();
        employerMode = false;
    }

    // ---- long-press explanations
    private <T extends View> T explain(T view, final String title, final String body) {
        view.setOnLongClickListener(v -> {
            if (!helpEnabled()) return false;
            if (hapticsEnabled()) v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            showHelp(title, body);
            return true;
        });
        return view;
    }

    private void showHelp(String title, String body) {
        LinearLayout box = column();
        box.setPadding(dp(22), dp(20), dp(22), dp(16));
        box.setBackground(shape(panel, 26));
        box.addView(text("💡  " + title, 19, gold, true));
        space(box, 10);
        TextView message = text(body, 15, ink, false);
        message.setLineSpacing(0f, 1.1f);
        box.addView(message);
        space(box, 10);
        box.addView(text("Un appui long sur un bouton affiche son explication. Désactivable dans Paramètres.", 11, muted, false));
        space(box, 12);
        final AlertDialog dialog = new AlertDialog.Builder(this).setView(box).create();
        box.addView(button("Compris", true, dialog::dismiss), new LinearLayout.LayoutParams(-1, dp(48)));
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(420)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private String actionHelp(String label) {
        if ("Retour".equals(label)) return "Revient à la carte précédente si vous avez glissé trop vite.";
        if ("Ignorer".equals(label)) return "Passe cette carte sans la sauvegarder. Même effet qu'un glissement vers la gauche.";
        if ("Sauvegarder".equals(label)) return "Garde cette carte dans votre profil pour la retrouver plus tard. Même effet qu'un glissement vers la droite.";
        if ("Contacter".equals(label)) return "Offre ANETI : ouvre l'annonce officielle pour postuler. Profil de démonstration : le contact n'est pas encore actif.";
        if ("Partager".equals(label)) return "Envoie les informations de cette carte par message ou réseau social.";
        return label;
    }

    // ---- tips on the home screen
    private final String[] seekerTips = {
        "Glissez une carte à droite pour la sauvegarder, à gauche pour la passer.",
        "Touchez une carte pour voir tous les détails de l'offre.",
        "Appui long sur un bouton ou une carte : une explication s'affiche.",
        "Les offres ANETI viennent du site officiel : vérifiez toujours la date limite.",
        "Ajoutez votre photo et votre CV dans Profil pour être prêt à postuler.",
        "Ne payez jamais pour obtenir un emploi : méfiez-vous des offres suspectes."
    };
    private final String[] recruiterTips = {
        "Précisez le poste, le lieu et le salaire pour attirer de bons profils.",
        "Les profils affichés sont fictifs : aucun candidat réel n'est contacté.",
        "Votre brouillon d'offre reste sur ce téléphone tant qu'il n'est pas publié.",
        "Appui long sur un bouton : une explication s'affiche.",
        "Ne demandez jamais d'argent à un candidat."
    };

    private void addTipRow() {
        if (!tipsEnabled() || tipDismissed) return;
        String[] tips = employerMode ? recruiterTips : seekerTips;
        SharedPreferences prefs = settings();
        int n = prefs.getInt("tip_index", 0);
        prefs.edit().putInt("tip_index", n + 1).apply();
        LinearLayout box = row();
        box.setPadding(dp(12), dp(8), dp(6), dp(8));
        box.setBackground(shape(Color.argb(40, 255, 190, 45), 14));
        box.addView(text("💡 " + tips[Math.abs(n) % tips.length], 12, Color.rgb(255, 226, 150), false),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = text("✕", 14, muted, true);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(12), dp(4), dp(12), dp(4));
        close.setContentDescription("Masquer l'astuce");
        close.setOnClickListener(v -> {
            tipDismissed = true;
            showHome();
        });
        box.addView(close);
        explain(box, "Astuces", "Un petit conseil s'affiche ici à chaque visite. Touchez ✕ pour le masquer, ou désactivez-les dans Paramètres.");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(6);
        content.addView(box, params);
    }

    private void tipSection(String title, String[] lines) {
        LinearLayout c = column();
        c.setPadding(dp(17), dp(15), dp(17), dp(15));
        c.setBackground(shape(panel, 18));
        c.addView(text(title, 18, gold, true));
        for (String line : lines) {
            space(c, 8);
            c.addView(text("•  " + line, 14, ink, false));
        }
        content.addView(c);
        space(content, 12);
    }

    private void showTips() {
        prepareScreen(false, this::showTips);
        base("Conseils et aide", "Bien utiliser TUN-JOBS, en toute sécurité.");
        tipSection("Pour les candidats", new String[] {
            "Glissez à droite pour sauvegarder une offre, à gauche pour la passer.",
            "Touchez une carte pour lire le détail complet.",
            "Complétez votre profil : photo claire, téléphone, expérience et CV.",
            "Postulez vite : vérifiez la date limite sur l'annonce officielle."
        });
        tipSection("Pour les recruteurs", new String[] {
            "Indiquez si vous recrutez seul(e) ou pour une société.",
            "Une société renseigne sa raison sociale, son matricule fiscal et un contact.",
            "Décrivez clairement le poste, le lieu, le contrat et le salaire.",
            "Les profils de démonstration sont fictifs : aucun contact réel."
        });
        tipSection("Éviter les arnaques", new String[] {
            "Ne payez jamais de frais pour obtenir un emploi ou un visa de travail.",
            "Vérifiez l'offre sur le site officiel (ANETI, concours publics).",
            "Ne partagez pas vos documents d'identité ni vos données bancaires.",
            "Méfiez-vous des promesses de salaire trop belles pour être vraies."
        });
        tipSection("Gestes et appui long", new String[] {
            "Appui long sur un bouton, un onglet ou une carte : une fenêtre l'explique.",
            "Vous pouvez désactiver cette aide et les astuces dans Paramètres."
        });
        content.addView(button("Revoir le tutoriel", false, () -> showWelcomeStep(0)), new LinearLayout.LayoutParams(-1, dp(54)));
        space(content, 8);
        content.addView(button("Ouvrir les paramètres", true, this::showSettings), new LinearLayout.LayoutParams(-1, dp(54)));
    }

    // ---- settings widgets
    private void sectionTitle(String title) {
        content.addView(text(title.toUpperCase(Locale.ROOT), 12, gold, true));
        space(content, 8);
    }

    private void switchRow(String title, String detail, final String key, boolean defaultValue) {
        LinearLayout r = row();
        r.setPadding(dp(16), dp(12), dp(12), dp(12));
        r.setBackground(shape(panel, 16));
        LinearLayout labels = column();
        labels.addView(text(title, 16, ink, true));
        space(labels, 2);
        labels.addView(text(detail, 12, muted, false));
        r.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        final Switch toggle = new Switch(this);
        toggle.setChecked(settings().getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            settings().edit().putBoolean(key, checked).apply();
            if ("tips".equals(key) && checked) tipDismissed = false;
        });
        r.addView(toggle);
        r.setOnClickListener(v -> toggle.toggle());
        content.addView(r, new LinearLayout.LayoutParams(-1, -2));
        space(content, 8);
    }

    private void choiceRow(String title, String detail, final String key, String[] options, int defaultValue) {
        LinearLayout box = column();
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackground(shape(panel, 16));
        box.addView(text(title, 16, ink, true));
        space(box, 2);
        box.addView(text(detail, 12, muted, false));
        space(box, 10);
        LinearLayout options2 = row();
        options2.setBackground(shape(Color.rgb(16, 18, 23), 24));
        options2.setPadding(dp(3), dp(3), dp(3), dp(3));
        int current = settings().getInt(key, defaultValue);
        for (int i = 0; i < options.length; i++) {
            final int value = i;
            options2.addView(segment(options[i], i == current, () -> {
                settings().edit().putInt(key, value).apply();
                refreshScreen();
            }), new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        box.addView(options2, new LinearLayout.LayoutParams(-1, -2));
        content.addView(box, new LinearLayout.LayoutParams(-1, -2));
        space(content, 8);
    }

    // ================================================================ recruiter identification
    private boolean hasEmployerProfile() {
        return getSharedPreferences(PREFS_EMPLOYER, MODE_PRIVATE).getBoolean("done", false);
    }

    private void requireEmployerProfile(Runnable next) {
        if (hasEmployerProfile()) next.run();
        else showEmployerType(next);
    }

    private void chooseEmployerMode() {
        requireEmployerProfile(() -> {
            employerMode = true;
            showHome();
        });
    }

    private String employerDisplayName() {
        SharedPreferences ep = getSharedPreferences(PREFS_EMPLOYER, MODE_PRIVATE);
        return "company".equals(ep.getString("type", "")) ? ep.getString("legal_name", "") : ep.getString("name", "");
    }

    private String employerGovernorate() {
        return getSharedPreferences(PREFS_EMPLOYER, MODE_PRIVATE).getString("governorate", defaultGovernorate());
    }

    private int digitCount(String value) {
        int n = 0;
        for (int i = 0; i < value.length(); i++) if (Character.isDigit(value.charAt(i))) n++;
        return n;
    }

    private EditText labeledField(LinearLayout parent, String label, String hint, String value, int inputType) {
        parent.addView(text(label, 13, muted, true));
        space(parent, 4);
        EditText field = draftField(hint, false);
        if (inputType != 0) field.setInputType(inputType);
        if (value != null && !value.isEmpty()) field.setText(value);
        parent.addView(field, new LinearLayout.LayoutParams(-1, dp(52)));
        space(parent, 10);
        return field;
    }

    private View typeCard(String icon, String title, String detail, boolean isSelected, final Runnable action) {
        LinearLayout card = row();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = shape(isSelected ? Color.rgb(45, 38, 22) : panel, 20);
        bg.setStroke(dp(2), isSelected ? gold : Color.rgb(45, 48, 56));
        card.setBackground(bg);
        TextView iconView = text(icon, 30, Color.WHITE, false);
        iconView.setGravity(Gravity.CENTER);
        card.addView(iconView, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout labels = column();
        labels.addView(text(title, 17, isSelected ? gold : ink, true));
        space(labels, 3);
        labels.addView(text(detail, 13, muted, false));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(10);
        card.addView(labels, labelParams);
        card.addView(text(isSelected ? "●" : "○", 20, isSelected ? gold : muted, true));
        card.setOnClickListener(v -> action.run());
        explain(card, title, detail + " Votre choix adapte le formulaire : un particulier remplit peu d'informations, une société en fournit davantage.");
        return card;
    }

    private void showEmployerType(final Runnable next) {
        prepareScreen(false, () -> showEmployerType(next));
        base("Qui recrute ?", "Choisissez votre situation pour adapter le formulaire.", TAB_EMPLOYERS, true);
        final SharedPreferences ep = getSharedPreferences(PREFS_EMPLOYER, MODE_PRIVATE);
        final String[] choice = {ep.getString("type", "")};
        content.addView(text("Avant de recruter, dites-nous si vous recrutez seul(e) ou pour une société. Nous vous demanderons les bonnes informations.", 14, muted, false));
        space(content, 14);
        final LinearLayout cardsBox = column();
        final LinearLayout formBox = column();
        content.addView(cardsBox);
        space(content, 10);
        content.addView(formBox);
        final Runnable[] render = new Runnable[1];
        render[0] = () -> {
            cardsBox.removeAllViews();
            cardsBox.addView(typeCard("👤", "Je recrute seul(e)", "Particulier, artisan, commerçant indépendant, famille…",
                    "alone".equals(choice[0]), () -> {
                        choice[0] = "alone";
                        render[0].run();
                    }));
            space(cardsBox, 10);
            cardsBox.addView(typeCard("🏢", "Je représente une société", "Entreprise, association, établissement public…",
                    "company".equals(choice[0]), () -> {
                        choice[0] = "company";
                        render[0].run();
                    }));
            formBox.removeAllViews();
            if ("alone".equals(choice[0])) buildAloneForm(formBox, ep, next);
            else if ("company".equals(choice[0])) buildCompanyForm(formBox, ep, next);
            else formBox.addView(text("Choisissez une option ci-dessus pour continuer.", 13, muted, false));
        };
        render[0].run();
    }

    private void buildAloneForm(LinearLayout box, final SharedPreferences ep, final Runnable next) {
        final EditText name = labeledField(box, "Nom et prénom *", "Votre nom", ep.getString("name", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final EditText phone = labeledField(box, "Téléphone *", "Ex. 22 123 456", ep.getString("phone", ""),
                InputType.TYPE_CLASS_PHONE);
        final Spinner gov = filterSpinner(GOVERNORATES);
        addSpinnerTo(box, "Gouvernorat *", gov);
        setSpinnerValue(gov, ep.getString("governorate", defaultGovernorate()));
        final Spinner need = filterSpinner(new String[] {
            "Aide à domicile / garde d'enfants", "Travaux / chantier", "Agriculture",
            "Commerce / boutique", "Services / livraison", "Autre"});
        addSpinnerTo(box, "Je recrute pour *", need);
        setSpinnerValue(need, ep.getString("need", ""));
        box.addView(text("Pour votre sécurité : ne demandez jamais d'argent à un candidat. Ces informations restent sur ce téléphone et ne sont ni vérifiées ni publiées pour l'instant.", 12, muted, false));
        space(box, 12);
        box.addView(button("Enregistrer et continuer", true, () -> {
            String nameValue = name.getText().toString().trim();
            String phoneValue = phone.getText().toString().trim();
            if (nameValue.isEmpty() || phoneValue.isEmpty()) {
                Toast.makeText(this, "Le nom et le téléphone sont obligatoires.", Toast.LENGTH_LONG).show();
                return;
            }
            if (digitCount(phoneValue) < 8) {
                Toast.makeText(this, "Numéro de téléphone incomplet (8 chiffres minimum).", Toast.LENGTH_LONG).show();
                return;
            }
            ep.edit().clear()
                    .putString("type", "alone")
                    .putString("name", nameValue)
                    .putString("phone", phoneValue)
                    .putString("governorate", selected(gov))
                    .putString("need", selected(need))
                    .putBoolean("done", true)
                    .apply();
            Toast.makeText(this, "Profil recruteur enregistré sur ce téléphone.", Toast.LENGTH_SHORT).show();
            next.run();
        }), new LinearLayout.LayoutParams(-1, dp(56)));
    }

    private void buildCompanyForm(LinearLayout box, final SharedPreferences ep, final Runnable next) {
        box.addView(text("Informations de la société", 18, gold, true));
        space(box, 10);
        final EditText legalName = labeledField(box, "Raison sociale *", "Nom officiel de la société", ep.getString("legal_name", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final Spinner legalForm = filterSpinner(new String[] {
            "SARL", "SUARL", "SA", "Entreprise individuelle", "Association", "Établissement public", "Autre"});
        addSpinnerTo(box, "Forme juridique", legalForm);
        setSpinnerValue(legalForm, ep.getString("legal_form", ""));
        final EditText taxId = labeledField(box, "Matricule fiscal *", "Ex. 1234567/A/M/000", ep.getString("tax_id", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        box.addView(text("Le matricule fiscal figure sur vos documents officiels. Il servira à vérifier votre société avant toute publication.", 11, muted, false));
        space(box, 10);
        final EditText rne = labeledField(box, "Identifiant RNE (facultatif)", "Registre national des entreprises", ep.getString("rne", ""),
                InputType.TYPE_CLASS_TEXT);
        final Spinner sector = filterSpinner(new String[] {
            "Commerce", "Agriculture", "Bâtiment", "Informatique", "Santé", "Transport",
            "Industrie", "Hôtellerie / restauration", "Services", "Autre"});
        addSpinnerTo(box, "Secteur d'activité", sector);
        setSpinnerValue(sector, ep.getString("sector", ""));
        final Spinner size = filterSpinner(new String[] {
            "1 à 9 salariés", "10 à 49 salariés", "50 à 249 salariés", "250 salariés et plus"});
        addSpinnerTo(box, "Taille de la société", size);
        setSpinnerValue(size, ep.getString("size", ""));
        final Spinner gov = filterSpinner(GOVERNORATES);
        addSpinnerTo(box, "Gouvernorat *", gov);
        setSpinnerValue(gov, ep.getString("governorate", defaultGovernorate()));
        final EditText address = labeledField(box, "Adresse *", "Rue, ville", ep.getString("address", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        box.addView(text("Personne à contacter", 18, gold, true));
        space(box, 10);
        final EditText contactName = labeledField(box, "Nom du responsable *", "Nom et prénom", ep.getString("contact_name", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final EditText contactRole = labeledField(box, "Fonction", "Ex. Responsable RH, gérant", ep.getString("contact_role", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText phone = labeledField(box, "Téléphone *", "Ex. 22 123 456", ep.getString("phone", ""),
                InputType.TYPE_CLASS_PHONE);
        final EditText email = labeledField(box, "E-mail professionnel *", "contact@societe.tn", ep.getString("email", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText website = labeledField(box, "Site web (facultatif)", "www.societe.tn", ep.getString("website", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        box.addView(text("Ces informations restent sur ce téléphone. Aucune vérification automatique n'est faite pour l'instant : la société sera vérifiée avant toute publication d'offre.", 12, muted, false));
        space(box, 12);
        box.addView(button("Enregistrer et continuer", true, () -> {
            String nameValue = legalName.getText().toString().trim();
            String taxValue = taxId.getText().toString().trim();
            String addressValue = address.getText().toString().trim();
            String contactValue = contactName.getText().toString().trim();
            String phoneValue = phone.getText().toString().trim();
            String emailValue = email.getText().toString().trim();
            if (nameValue.isEmpty() || taxValue.isEmpty() || addressValue.isEmpty() || contactValue.isEmpty()
                    || phoneValue.isEmpty() || emailValue.isEmpty()) {
                Toast.makeText(this, "Complétez les champs obligatoires (*).", Toast.LENGTH_LONG).show();
                return;
            }
            if (digitCount(taxValue) < 7) {
                Toast.makeText(this, "Matricule fiscal incomplet (7 chiffres minimum).", Toast.LENGTH_LONG).show();
                return;
            }
            if (digitCount(phoneValue) < 8) {
                Toast.makeText(this, "Numéro de téléphone incomplet (8 chiffres minimum).", Toast.LENGTH_LONG).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
                Toast.makeText(this, "Adresse e-mail invalide.", Toast.LENGTH_LONG).show();
                return;
            }
            ep.edit().clear()
                    .putString("type", "company")
                    .putString("legal_name", nameValue)
                    .putString("legal_form", selected(legalForm))
                    .putString("tax_id", taxValue)
                    .putString("rne", rne.getText().toString().trim())
                    .putString("sector", selected(sector))
                    .putString("size", selected(size))
                    .putString("governorate", selected(gov))
                    .putString("address", addressValue)
                    .putString("contact_name", contactValue)
                    .putString("contact_role", contactRole.getText().toString().trim())
                    .putString("phone", phoneValue)
                    .putString("email", emailValue)
                    .putString("website", website.getText().toString().trim())
                    .putBoolean("done", true)
                    .apply();
            Toast.makeText(this, "Profil société enregistré sur ce téléphone.", Toast.LENGTH_SHORT).show();
            next.run();
        }), new LinearLayout.LayoutParams(-1, dp(56)));
    }

    private void addEmployerSummary() {
        final SharedPreferences ep = getSharedPreferences(PREFS_EMPLOYER, MODE_PRIVATE);
        boolean company = "company".equals(ep.getString("type", ""));
        LinearLayout card = column();
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable bg = shape(Color.rgb(34, 31, 22), 22);
        bg.setStroke(dp(1), Color.argb(120, 255, 190, 45));
        card.setBackground(bg);
        card.addView(text(company ? "VOTRE SOCIÉTÉ" : "VOTRE PROFIL RECRUTEUR", 11, gold, true));
        space(card, 6);
        card.addView(text((company ? "🏢  " : "👤  ") + employerDisplayName(), 20, Color.WHITE, true));
        space(card, 4);
        String detail = company
                ? ep.getString("legal_form", "") + " • " + ep.getString("sector", "") + " • " + ep.getString("governorate", "")
                : "Particulier • " + ep.getString("need", "") + " • " + ep.getString("governorate", "");
        card.addView(text(detail, 13, Color.rgb(220, 228, 238), false));
        space(card, 4);
        card.addView(text("☎  " + ep.getString("phone", ""), 13, muted, false));
        space(card, 10);
        card.addView(button("Modifier mon profil recruteur", false, () -> showEmployerType(this::showEmployerSpace)),
                new LinearLayout.LayoutParams(-1, dp(46)));
        explain(card, "Votre profil recruteur",
                "Ces informations (seul ou société) servent à préparer vos offres. Elles restent sur ce téléphone. Touchez le bouton pour les modifier.");
        content.addView(card);
        space(content, 16);
    }

    // ================================================================ generated avatars
    private long avatarSeed(Candidate c) {
        return (c.name.hashCode() * 1000003L) ^ avatarSalt;
    }

    /** Cartoon portrait drawn with Canvas: no network, no real person, different on every launch. */
    private static class AvatarDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private final int bgA, bgB, skin, hair, shirt, hairStyle;
        private final boolean glasses, beard;

        AvatarDrawable(long seed) {
            Random r = new Random(seed);
            float hue = r.nextInt(360);
            bgA = Color.HSVToColor(new float[] {hue, 0.55f, 0.60f});
            bgB = Color.HSVToColor(new float[] {(hue + 40f) % 360f, 0.70f, 0.22f});
            int[] skins = {Color.rgb(246, 214, 186), Color.rgb(234, 190, 150), Color.rgb(205, 152, 112),
                    Color.rgb(160, 110, 76), Color.rgb(112, 74, 52)};
            int[] hairs = {Color.rgb(24, 20, 20), Color.rgb(58, 38, 28), Color.rgb(96, 62, 36),
                    Color.rgb(168, 124, 64), Color.rgb(150, 150, 154)};
            int[] shirts = {Color.rgb(44, 82, 160), Color.rgb(196, 70, 66), Color.rgb(36, 120, 96),
                    Color.rgb(232, 232, 236), Color.rgb(40, 40, 48), Color.rgb(230, 170, 50), Color.rgb(120, 70, 150)};
            skin = skins[r.nextInt(skins.length)];
            hair = hairs[r.nextInt(hairs.length)];
            shirt = shirts[r.nextInt(shirts.length)];
            hairStyle = r.nextInt(5);
            glasses = r.nextInt(4) == 0;
            beard = hairStyle != 1 && hairStyle != 3 && r.nextInt(3) == 0;
        }

        private static int darker(int c) {
            return Color.rgb((int) (Color.red(c) * 0.86f), (int) (Color.green(c) * 0.86f), (int) (Color.blue(c) * 0.86f));
        }

        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            float w = b.width(), h = b.height();
            if (w <= 0f || h <= 0f) return;
            float cx = b.left + w / 2f;
            float u = Math.min(w, h * 0.78f);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, bgA, bgB, Shader.TileMode.CLAMP));
            c.drawRect(b, paint);
            paint.setShader(null);
            paint.setColor(Color.argb(30, 255, 255, 255));
            c.drawCircle(b.left + w * 0.16f, b.top + h * 0.18f, u * 0.26f, paint);
            c.drawCircle(b.left + w * 0.88f, b.top + h * 0.48f, u * 0.34f, paint);

            float headCy = b.top + h * 0.36f;
            float hrx = u * 0.20f, hry = u * 0.25f;
            float shoulderTop = headCy + hry + u * 0.14f;

            paint.setColor(shirt);
            rect.set(cx - u * 0.52f, shoulderTop, cx + u * 0.52f, shoulderTop + u * 0.9f);
            c.drawRoundRect(rect, u * 0.30f, u * 0.30f, paint);

            int neckColor = darker(skin);
            paint.setColor(neckColor);
            rect.set(cx - u * 0.07f, headCy + hry * 0.6f, cx + u * 0.07f, shoulderTop + u * 0.06f);
            c.drawRect(rect, paint);
            path.reset();
            path.moveTo(cx - u * 0.11f, shoulderTop);
            path.lineTo(cx + u * 0.11f, shoulderTop);
            path.lineTo(cx, shoulderTop + u * 0.15f);
            path.close();
            c.drawPath(path, paint);

            paint.setColor(hair);
            if (hairStyle == 1) {
                rect.set(cx - hrx * 1.22f, headCy - hry * 1.05f, cx + hrx * 1.22f, headCy + hry * 2.1f);
                c.drawRoundRect(rect, hrx, hrx, paint);
            } else if (hairStyle == 3) {
                c.drawCircle(cx, headCy - hry * 1.12f, hrx * 0.5f, paint);
            }

            paint.setColor(skin);
            rect.set(cx - hrx, headCy - hry, cx + hrx, headCy + hry);
            c.drawOval(rect, paint);
            c.drawCircle(cx - hrx, headCy + hry * 0.08f, hrx * 0.16f, paint);
            c.drawCircle(cx + hrx, headCy + hry * 0.08f, hrx * 0.16f, paint);

            if (beard) {
                paint.setColor(hair);
                path.reset();
                rect.set(cx - hrx * 0.93f, headCy - hry * 0.2f, cx + hrx * 0.93f, headCy + hry * 1.0f);
                path.addArc(rect, 0f, 180f);
                path.close();
                c.drawPath(path, paint);
            }

            paint.setColor(hairStyle == 4 ? shirt : hair);
            rect.set(cx - hrx * 1.04f, headCy - hry * 1.08f, cx + hrx * 1.04f, headCy + hry * 0.5f);
            path.reset();
            path.addArc(rect, 180f, 180f);
            path.close();
            c.drawPath(path, paint);
            if (hairStyle == 2) {
                paint.setColor(hair);
                for (int i = 0; i < 7; i++) {
                    double a = Math.toRadians(190 + i * 27);
                    float px = cx + (float) Math.cos(a) * hrx * 1.04f;
                    float py = headCy - hry * 0.29f + (float) Math.sin(a) * hry * 0.79f;
                    c.drawCircle(px, py, hrx * 0.36f, paint);
                }
            }
            if (hairStyle == 4) {
                paint.setColor(shirt);
                rect.set(cx - hrx * 1.12f, headCy - hry * 0.36f, cx + hrx * 1.55f, headCy - hry * 0.22f);
                c.drawRoundRect(rect, u * 0.02f, u * 0.02f, paint);
            }

            paint.setColor(neckColor);
            rect.set(cx - hrx * 0.08f, headCy + hry * 0.25f, cx + hrx * 0.08f, headCy + hry * 0.38f);
            c.drawOval(rect, paint);

            float eyeY = headCy + hry * 0.14f, eyeDx = hrx * 0.42f, er = hrx * 0.09f;
            paint.setColor(Color.rgb(32, 27, 29));
            c.drawCircle(cx - eyeDx, eyeY, er, paint);
            c.drawCircle(cx + eyeDx, eyeY, er, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(u * 0.012f);
            paint.setColor(hairStyle == 4 ? Color.rgb(60, 42, 32) : hair);
            c.drawLine(cx - eyeDx - er * 1.7f, eyeY - er * 2.7f, cx - eyeDx + er * 1.7f, eyeY - er * 3.0f, paint);
            c.drawLine(cx + eyeDx - er * 1.7f, eyeY - er * 3.0f, cx + eyeDx + er * 1.7f, eyeY - er * 2.7f, paint);

            paint.setStrokeWidth(u * 0.016f);
            paint.setColor(beard ? Color.rgb(240, 214, 204) : Color.rgb(128, 58, 56));
            rect.set(cx - hrx * 0.34f, headCy + hry * 0.36f, cx + hrx * 0.34f, headCy + hry * 0.70f);
            c.drawArc(rect, 20f, 140f, false, paint);

            if (glasses) {
                paint.setStrokeWidth(u * 0.013f);
                paint.setColor(Color.rgb(24, 24, 30));
                c.drawCircle(cx - eyeDx, eyeY, hrx * 0.27f, paint);
                c.drawCircle(cx + eyeDx, eyeY, hrx * 0.27f, paint);
                c.drawLine(cx - eyeDx + hrx * 0.27f, eyeY, cx + eyeDx - hrx * 0.27f, eyeY, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }

        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }


    // ================================================================ Supabase authentication
    private SharedPreferences authPrefs() {
        return getSharedPreferences(PREFS_AUTH, MODE_PRIVATE);
    }

    private boolean hasAuthSession() {
        return !authPrefs().getString("access_token", "").isEmpty();
    }

    private void logout() {
        authPrefs().edit().clear().apply();
        authSignupMode = false;
        screenHistory.clear();
        showAuth();
    }

    private EditText authField(String label, String hint, int inputType) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(16);
        field.setTextColor(ink);
        field.setHintTextColor(muted);
        field.setHint(hint);
        field.setInputType(inputType);
        field.setPadding(dp(16), dp(12), dp(16), dp(12));
        field.setBackground(shape(panel, 16));
        field.setMinimumHeight(dp(54));
        content.addView(text(label, 13, muted, true));
        space(content, 5);
        content.addView(field, new LinearLayout.LayoutParams(-1, dp(56)));
        space(content, 12);
        return field;
    }

    private void showAuth() {
        prepareScreen(true, this::showAuth);
        chromeless = true;
        base("Bienvenue sur TUN-JOBS", "Connectez-vous ou créez votre compte pour continuer.", -1, true);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout brand = column();
        brand.setGravity(Gravity.CENTER);
        brand.setPadding(dp(18), dp(14), dp(18), dp(18));
        brand.setBackground(shape(Color.rgb(25, 27, 33), 22));
        TextView logo = text("TUN-JOBS", 30, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        brand.addView(logo, new LinearLayout.LayoutParams(-1, -2));
        TextView tagline = text("EMPLOI • RECRUTEMENT • TUNISIE", 12, gold, true);
        tagline.setGravity(Gravity.CENTER);
        brand.addView(tagline);
        content.addView(brand, new LinearLayout.LayoutParams(-1, -2));
        space(content, 20);

        LinearLayout modes = row();
        modes.setBackground(shape(panel, 24));
        modes.setPadding(dp(4), dp(4), dp(4), dp(4));
        modes.addView(segment("Se connecter", !authSignupMode, () -> { authSignupMode = false; showAuth(); }),
                new LinearLayout.LayoutParams(0, dp(48), 1));
        modes.addView(segment("Créer un compte", authSignupMode, () -> { authSignupMode = true; showAuth(); }),
                new LinearLayout.LayoutParams(0, dp(48), 1));
        content.addView(modes, new LinearLayout.LayoutParams(-1, -2));
        space(content, 18);

        final EditText name = authSignupMode
                ? authField("Nom et prénom", "Votre nom complet", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                : null;
        final EditText email = authField("Adresse e-mail", "nom@exemple.com",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText password = authField("Mot de passe", "Minimum 6 caractères",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final EditText confirm = authSignupMode
                ? authField("Confirmer le mot de passe", "Répétez votre mot de passe",
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                : null;
        password.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        if (confirm != null) confirm.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        space(content, 4);
        content.addView(button(authSignupMode ? "Créer mon compte" : "Se connecter", true, () -> {
            String emailValue = email.getText().toString().trim();
            String passwordValue = password.getText().toString();
            String nameValue = name == null ? "" : name.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
                email.setError("Adresse e-mail invalide.");
                return;
            }
            if (passwordValue.length() < 6) {
                password.setError("Le mot de passe doit contenir au moins 6 caractères.");
                return;
            }
            if (authSignupMode && (nameValue.isEmpty() || !passwordValue.equals(confirm.getText().toString()))) {
                if (nameValue.isEmpty()) name.setError("Indiquez votre nom.");
                else confirm.setError("Les mots de passe ne correspondent pas.");
                return;
            }
            runAuthRequest(authSignupMode, nameValue, emailValue, passwordValue);
        }), new LinearLayout.LayoutParams(-1, dp(58)));
        space(content, 12);
        TextView privacy = text("Connexion sécurisée avec Supabase. Votre mot de passe n'est jamais enregistré dans l'application.", 12, muted, false);
        privacy.setGravity(Gravity.CENTER);
        content.addView(privacy, new LinearLayout.LayoutParams(-1, -2));
        space(content, 14);
        content.addView(button("Retour à l'accueil de bienvenue", false, () -> showWelcomeStep(0)),
                new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void runAuthRequest(final boolean signup, final String name, final String email, final String password) {
        final String endpoint = signup ? "signup" : "token?grant_type=password";
        Toast.makeText(this, signup ? "Création du compte…" : "Connexion…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", email);
                payload.put("password", password);
                if (signup) {
                    JSONObject data = new JSONObject();
                    data.put("full_name", name);
                    payload.put("data", data);
                }
                JSONObject result = supabaseAuthRequest(endpoint, payload);
                JSONObject session = result.optJSONObject("session");
                if (session == null) session = result;
                String access = session.optString("access_token", "");
                String refresh = session.optString("refresh_token", "");
                JSONObject user = result.optJSONObject("user");
                if (user == null) user = session.optJSONObject("user");
                if (!access.isEmpty()) {
                    SharedPreferences.Editor editor = authPrefs().edit()
                            .putString("access_token", access)
                            .putString("refresh_token", refresh)
                            .putString("email", email)
                            .putBoolean("email_confirmed", user == null || user.optString("email_confirmed_at", "").length() > 0);
                    if (user != null) editor.putString("user_id", user.optString("id", ""));
                    if (signup) editor.putString("full_name", name);
                    editor.apply();
                    runOnUiThread(() -> {
                        getSharedPreferences("tun_jobs", MODE_PRIVATE).edit().putBoolean("welcome_done", true).apply();
                        employerMode = authPrefs().getBoolean("pending_employer_mode", false);
                        authPrefs().edit().remove("pending_employer_mode").apply();
                        screenHistory.clear();
                        Toast.makeText(this, "Connexion réussie. Bienvenue sur TUN-JOBS !", Toast.LENGTH_LONG).show();
                        if (employerMode) chooseEmployerMode(); else showHome();
                    });
                } else if (signup) {
                    runOnUiThread(() -> {
                        authSignupMode = false;
                        showAuth();
                        Toast.makeText(this, "Compte créé. Vérifiez votre e-mail pour confirmer votre adresse, puis connectez-vous.", Toast.LENGTH_LONG).show();
                    });
                } else {
                    throw new Exception("La réponse du serveur ne contient pas de session.");
                }
            } catch (Exception error) {
                String message = error.getMessage() == null ? "Erreur de connexion." : error.getMessage();
                runOnUiThread(() -> Toast.makeText(this, "Authentification : " + message, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private JSONObject supabaseAuthRequest(String endpoint, JSONObject payload) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(SUPABASE_URL + "/auth/v1/" + endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        byte[] body = payload.toString().getBytes("UTF-8");
        try (java.io.OutputStream request = connection.getOutputStream()) {
            request.write(body);
            request.flush();
        }
        int status = connection.getResponseCode();
        java.io.InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (stream != null) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
            stream.close();
        }
        connection.disconnect();
        String response = new String(output.toByteArray(), "UTF-8");
        JSONObject json = response.isEmpty() ? new JSONObject() : new JSONObject(response);
        if (status < 200 || status >= 300) {
            String reason = json.optString("msg", json.optString("message",
                    json.optString("error_description", json.optString("error", "Erreur serveur HTTP " + status))));
            throw new Exception(reason);
        }
        return json;
    }

    private void showNotice() {
        prepareScreen(false, this::showNotice);
        base("Fonction en préparation", "Cette première version est une base de démonstration.");
        content.addView(text("Les candidatures, comptes et publications seront ajoutés après la mise en place du backend sécurisé.", 15, muted, false));
        space(content, 14);
        content.addView(button("Retour", true, this::showHome));
    }
}
