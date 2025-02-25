package com.serenitysystems.livable.ui.settings

import FAQCategoryAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R

class FAQFragment : Fragment() {

    private val categorizedFaqList = listOf(
        "🏠 Allgemeine Fragen zur App" to listOf(
            "Was ist Livable?" to "Livable ist eine App zur Organisation von WGs. Sie hilft dir, Haushaltsaufgaben zu verwalten, Einkäufe zu planen, Ausgaben zu tracken und eine gemeinsame WG-Struktur zu pflegen.",
            "Kann ich Livable auch alleine nutzen?" to "Ja! Du kannst Livable auch ohne eine WG nutzen, z. B. für deine persönliche To-Do-Liste."
        ),
        "👥 WG-System" to listOf(
            "Wie kann ich eine WG erstellen oder beitreten?" to "Du kannst entweder eine neue WG erstellen oder einer bestehenden WG beitreten. Die WG-ID zum Beitritt findest du in der WG-Info oder in deinem Profil.",
            "Welche Rechte hat ein WG-Leiter?" to "Der WG-Leiter kann Mitglieder hinzufügen oder entfernen, die WG löschen oder einen neuen WG-Leiter bestimmen.",
            "Was passiert, wenn ich eine WG lösche?" to "Bevor du eine WG löschst, musst du einen neuen WG-Leiter festlegen. Falls du der einzige Leiter bist, kannst du die WG erst nach Auflösung löschen."
        ),
        "📅 Wochenplan & Aufgaben" to listOf(
            "Welche Arten von Aufgaben gibt es?" to "🔵 Normale Aufgaben: Reguläre Aufgaben, die dir oder anderen zugewiesen sind.\n🟢 Übernehmbare Aufgaben: Aufgaben, die jeder in der WG übernehmen kann.\n🔴 Überfällige Aufgaben: Aufgaben, die nicht rechtzeitig erledigt wurden.",
            "Kann ich mich von einer Aufgabe abmelden?" to "Ja! Falls du eine Aufgabe nicht mehr erledigen kannst, kannst du sie freigeben. Sie wird dann als übernehmbare Aufgabe markiert.",
            "Was passiert mit überfälligen Aufgaben?" to "Überfällige Aufgaben erscheinen rot im Wochenplan. Wenn du sie nicht erledigst, werden die Hälfte der Punkte für diese Aufgabe von deinem Punktstand abgezogen."
        ),
        "🛒 Einkaufsliste" to listOf(
            "Wie funktioniert die Einkaufsliste?" to "Die Einkaufsliste hat verschiedene Kategorien wie Lebensmittel oder Haushaltsartikel. Falls du einen Einkauf verschieben möchtest, kannst du das Datum für ein Item ändern.",
            "Kann ich meine Einkäufe mit anderen WG-Mitgliedern teilen?" to "Ja! Deine WG-Mitglieder sehen die Liste und können gemeinsam Einträge hinzufügen oder erledigte Einkäufe abhaken."
        ),
        "💰 Haushaltsbuch" to listOf(
            "Wie funktioniert das Haushaltsbuch?" to "Das Haushaltsbuch hilft dir, Einnahmen und Ausgaben in der WG zu tracken. Es zeigt dir eine Übersicht, wie viel Geld von jedem ausgegeben wurde.",
            "Können alle WG-Mitglieder das Haushaltsbuch sehen?" to "Ja, alle Mitglieder der WG können das Haushaltsbuch einsehen."
        ),
        "✅ To-Do-Liste" to listOf(
            "Wie funktioniert die To-Do-Liste?" to "Du kannst Aufgaben für heute, morgen, diese Woche oder später planen.\n🟡 Gelb = Mittlere Priorität\n🔴 Rot = Hohe Priorität",
            "Unterscheidet sich die To-Do-Liste vom Wochenplan?" to "Ja! Die To-Do-Liste ist privat und nicht mit der WG verbunden. Der Wochenplan wird mit allen WG-Mitgliedern geteilt."
        ),
        "🏆 Punktesystem & Badges" to listOf(
            "Wie funktioniert das Punktesystem?" to "Jede erledigte Aufgabe bringt Punkte. Deine Punkte werden am Monatsanfang zurückgesetzt. Es gibt eine Lifetime-Punktebalance, die deine Gesamtpunkte speichert.",
            "Was passiert, wenn ich eine Aufgabe nicht erledige?" to "Wenn du eine Aufgabe nicht am geplanten Tag erledigst, verlierst du am nächsten Tag die Hälfte der Punkte. Die Aufgabe wird dann als 'Overdue' (überfällig) markiert.",
            "Was sind Badges und wie bekomme ich sie?" to "Badges werden basierend auf deinen monatlichen Punkten vergeben. Je mehr Punkte du im Monat sammelst, desto höher dein Badge-Rang."
        )
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_faq, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.faqRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = FAQCategoryAdapter(categorizedFaqList)

        return view
    }
}
