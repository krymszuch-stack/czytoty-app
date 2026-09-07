package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContactEntityType
import com.example.model.ContactSortMode
import com.example.model.OrganizationCategory
import com.example.model.PersonRelationship
import com.example.model.RiskSeverity
import com.example.model.UnifiedContact
import com.example.ui.components.RiskBadge
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppGreenContainer
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurpleLight
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Ekran Kontakty 2.0 (Ludzie + Organizacje)
 * Zgodny z sekcjami 33, 34, 35, 36, 37, 38 specyfikacji.
 */
@Composable
fun ContactsScreen(
  contacts: List<UnifiedContact>,
  currentSortMode: ContactSortMode,
  onSelectContact: (UnifiedContact) -> Unit,
  onTogglePin: (String) -> Unit,
  onToggleQuickContact: (String) -> Unit,
  onSetSortMode: (ContactSortMode) -> Unit,
  onReorderManual: (fromIndex: Int, toIndex: Int) -> Unit,
  onAddContact: (UnifiedContact) -> Unit,
  onAcceptSmartSuggestion: (String) -> Unit,
  onDismissSmartSuggestion: (String) -> Unit,
  onOpenSmsThread: (String) -> Unit,
  onBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var isSortMenuExpanded by remember { mutableStateOf(false) }
  var isAddDialogOpen by remember { mutableStateOf(false) }

  val filteredContacts = remember(contacts, searchQuery) {
    if (searchQuery.isBlank()) {
      contacts
    } else {
      val query = searchQuery.trim().lowercase()
      contacts.filter { contact ->
        contact.primaryDisplayName.lowercase().contains(query) ||
        contact.allPhoneNumbers.any { it.contains(query) } ||
        contact.smsSenderIds.any { it.lowercase().contains(query) } ||
        contact.firstName?.lowercase()?.contains(query) == true ||
        contact.lastName?.lowercase()?.contains(query) == true
      }
    }
  }

  // Szybkie kontakty (Sekcja 37)
  val quickContacts = remember(contacts) {
    contacts.filter { it.quickContactSlot }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(AppDarkBackground)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
      // 1. Nagłówek ekranu
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            if (onBack != null) {
              IconButton(
                onClick = onBack,
                modifier = Modifier
                  .padding(end = 4.dp)
                  .testTag("contacts_back_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Wstecz",
                  tint = AppTextPrimary
                )
              }
            }

            Column {
              Text(
                text = "Kontakty 2.0",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppTextPrimary
              )
              Text(
                text = "Ludzie i Organizacje",
                fontSize = 13.sp,
                color = AppTextSecondary
              )
            }
          }

          // Przycisk sortowania
          Box {
            OutlinedButton(
              onClick = { isSortMenuExpanded = true },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, AppBorder),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = AppPurpleLight),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("contacts_sort_button")
            ) {
              Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Sortuj",
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = currentSortMode.label.substringBefore(" ("),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }

            DropdownMenu(
              expanded = isSortMenuExpanded,
              onDismissRequest = { isSortMenuExpanded = false },
              modifier = Modifier.background(AppSurfaceVariant)
            ) {
              ContactSortMode.values().forEach { mode ->
                DropdownMenuItem(
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      RadioButton(
                        selected = currentSortMode == mode,
                        onClick = null
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = mode.label,
                        color = if (currentSortMode == mode) AppPurpleLight else AppTextPrimary,
                        fontWeight = if (currentSortMode == mode) FontWeight.Bold else FontWeight.Normal
                      )
                    }
                  },
                  onClick = {
                    onSetSortMode(mode)
                    isSortMenuExpanded = false
                  }
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pasek wyszukiwania
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Szukaj kontaktu, firmy lub numeru...", color = AppTextMuted, fontSize = 14.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = AppTextSecondary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Wyczyść", tint = AppTextSecondary)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("contacts_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))
      }

      // 2. SEKCJA 37: SZYBKIE KONTAKTY (Duże kafelki do szybkiego dzwonienia/SMS)
      if (quickContacts.isNotEmpty() && searchQuery.isBlank()) {
        item {
          Text(
            text = "SZYBKIE KONTAKTY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppPurpleLight,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            quickContacts.take(3).forEach { qc ->
              QuickContactCard(
                contact = qc,
                onClick = {
                  if (qc.singleClickToCall && qc.allPhoneNumbers.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${qc.allPhoneNumbers.first()}"))
                    context.startActivity(intent)
                  } else {
                    onSelectContact(qc)
                  }
                },
                onQuickCall = {
                  val number = qc.allPhoneNumbers.firstOrNull() ?: ""
                  if (number.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    context.startActivity(intent)
                  }
                },
                onQuickMessage = {
                  val addr = qc.allPhoneNumbers.firstOrNull() ?: qc.primaryIdentifier
                  onOpenSmsThread(addr)
                },
                modifier = Modifier.weight(1f)
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))
        }
      }

      // 3. INTELIGENTNE SUGESTIE (Sekcja 38: Smart but optional)
      val suggestionContact = contacts.firstOrNull { it.smartPinSuggestion != null && !it.isPinned }
      if (suggestionContact != null && searchQuery.isBlank()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppPurpleContainer.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, AppPurple.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 14.dp)
              .testTag("smart_pin_suggestion_banner")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(AppPurple),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.PushPin,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Inteligentna sugestia",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = AppPurpleLight
                )
                Text(
                  text = suggestionContact.smartPinSuggestion ?: "Często kontaktujesz się z tym odbiorcą.",
                  fontSize = 12.sp,
                  color = AppTextPrimary,
                  lineHeight = 16.sp
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              TextButton(
                onClick = { onAcceptSmartSuggestion(suggestionContact.id) },
                contentPadding = PaddingValues(horizontal = 8.dp)
              ) {
                Text("Przypnij", fontWeight = FontWeight.Bold, color = AppPurpleLight, fontSize = 12.sp)
              }

              IconButton(
                onClick = { onDismissSmartSuggestion(suggestionContact.id) },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Odrzuć", tint = AppTextSecondary, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      // 4. Lista kontaktów (Przypięte na samej górze z odrębną sekcją jeśli są)
      val pinnedList = filteredContacts.filter { it.isPinned }
      val regularList = filteredContacts.filter { !it.isPinned }

      if (pinnedList.isNotEmpty()) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.PushPin, contentDescription = null, tint = AppPurpleLight, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "PRZYPIĘTE (${pinnedList.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = AppPurpleLight,
              letterSpacing = 0.5.sp
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
        }

        itemsIndexed(pinnedList, key = { _, c -> "pin-${c.id}" }) { index, contact ->
          ContactRowItem(
            contact = contact,
            isManualSort = currentSortMode == ContactSortMode.MANUAL,
            canMoveUp = index > 0,
            canMoveDown = index < pinnedList.size - 1,
            onMoveUp = { onReorderManual(index, index - 1) },
            onMoveDown = { onReorderManual(index, index + 1) },
            onClick = { onSelectContact(contact) },
            onTogglePin = { onTogglePin(contact.id) },
            onToggleQuick = { onToggleQuickContact(contact.id) },
            onCall = {
              val number = contact.allPhoneNumbers.firstOrNull() ?: ""
              if (number.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                context.startActivity(intent)
              }
            },
            onMessage = {
              val addr = contact.allPhoneNumbers.firstOrNull() ?: contact.primaryIdentifier
              onOpenSmsThread(addr)
            }
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        if (regularList.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "POZOSTAŁE KONTAKTY (${regularList.size})",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextSecondary,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
          }
        }
      }

      // Regularne kontakty
      itemsIndexed(regularList, key = { _, c -> c.id }) { index, contact ->
        ContactRowItem(
          contact = contact,
          isManualSort = currentSortMode == ContactSortMode.MANUAL,
          canMoveUp = index > 0,
          canMoveDown = index < regularList.size - 1,
          onMoveUp = { onReorderManual(pinnedList.size + index, pinnedList.size + index - 1) },
          onMoveDown = { onReorderManual(pinnedList.size + index, pinnedList.size + index + 1) },
          onClick = { onSelectContact(contact) },
          onTogglePin = { onTogglePin(contact.id) },
          onToggleQuick = { onToggleQuickContact(contact.id) },
          onCall = {
            val number = contact.allPhoneNumbers.firstOrNull() ?: ""
            if (number.isNotBlank()) {
              val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
              context.startActivity(intent)
            }
          },
          onMessage = {
            val addr = contact.allPhoneNumbers.firstOrNull() ?: contact.primaryIdentifier
            onOpenSmsThread(addr)
          }
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      if (filteredContacts.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Person, contentDescription = null, tint = AppTextMuted, modifier = Modifier.size(48.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("Brak kontaktów spełniających kryteria", color = AppTextSecondary)
            }
          }
        }
      }
    }

    // Pływający przycisk dodawania kontaktu
    FloatingActionButton(
      onClick = { isAddDialogOpen = true },
      containerColor = AppPurple,
      contentColor = Color.White,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(20.dp)
        .testTag("add_contact_fab")
    ) {
      Icon(Icons.Default.Add, contentDescription = "Dodaj kontakt")
    }
  }

  // Dialog dodawania nowego kontaktu (Person lub Organization)
  if (isAddDialogOpen) {
    AddContactDialog(
      onDismiss = { isAddDialogOpen = false },
      onConfirm = { newContact ->
        onAddContact(newContact)
        isAddDialogOpen = false
      }
    )
  }
}

/**
 * Kafelek szybkiego kontaktu (Quick Contact - Sekcja 37)
 */
@Composable
fun QuickContactCard(
  contact: UnifiedContact,
  onClick: () -> Unit,
  onQuickCall: () -> Unit,
  onQuickMessage: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
    border = BorderStroke(1.dp, AppBorder),
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .clickable { onClick() }
      .testTag("quick_contact_${contact.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(
            if (contact.entityType == ContactEntityType.ORGANIZATION) AppPurpleContainer
            else AppGreenContainer
          ),
        contentAlignment = Alignment.Center
      ) {
        if (contact.entityType == ContactEntityType.ORGANIZATION) {
          Icon(Icons.Default.Business, contentDescription = null, tint = AppPurple, modifier = Modifier.size(24.dp))
        } else {
          Text(
            text = contact.avatarInitials,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppGreen
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = contact.primaryDisplayName,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = AppTextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = when {
          contact.relationship != null -> contact.relationship.label
          contact.organizationCategory != null -> contact.organizationCategory.displayName.substringBefore(" /")
          else -> if (contact.entityType == ContactEntityType.PERSON) "Osoba" else "Firma"
        },
        fontSize = 11.sp,
        color = AppTextSecondary,
        maxLines = 1
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        // Mini przycisk Zadzwoń (40x40 touch target)
        Surface(
          shape = CircleShape,
          color = AppGreen.copy(alpha = 0.15f),
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { onQuickCall() }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Phone, contentDescription = "Zadzwoń", tint = AppGreen, modifier = Modifier.size(18.dp))
          }
        }

        // Mini przycisk SMS
        Surface(
          shape = CircleShape,
          color = AppPurple.copy(alpha = 0.15f),
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { onQuickMessage() }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ChatBubble, contentDescription = "Wiadomość", tint = AppPurpleLight, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

/**
 * Wiersz listy kontaktu w Contacts 2.0
 */
@Composable
fun ContactRowItem(
  contact: UnifiedContact,
  isManualSort: Boolean,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onClick: () -> Unit,
  onTogglePin: () -> Unit,
  onToggleQuick: () -> Unit,
  onCall: () -> Unit,
  onMessage: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
    border = BorderStroke(
      1.dp,
      if (contact.isPinned) AppPurple.copy(alpha = 0.5f) else AppBorder
    ),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .clickable { onClick() }
      .testTag("contact_item_${contact.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. Awatar / Logo encji
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(
            when (contact.entityType) {
              ContactEntityType.ORGANIZATION -> Color(0xFF261D42)
              ContactEntityType.PERSON -> Color(0xFF193225)
            }
          ),
        contentAlignment = Alignment.Center
      ) {
        if (contact.entityType == ContactEntityType.ORGANIZATION) {
          Icon(
            imageVector = when (contact.organizationCategory) {
              OrganizationCategory.COURIER_LOGISTICS -> Icons.Default.LocalShipping
              OrganizationCategory.HEALTHCARE -> Icons.Default.MedicalServices
              OrganizationCategory.BANKING_FINANCE -> Icons.Default.Business
              else -> Icons.Default.Business
            },
            contentDescription = null,
            tint = AppPurpleLight,
            modifier = Modifier.size(24.dp)
          )
        } else {
          Text(
            text = contact.avatarInitials,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppGreen
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // 2. Informacje o kontakcie (Tożsamość komunikacyjna)
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = contact.primaryDisplayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.width(6.dp))

          // Badge typu encji
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (contact.entityType == ContactEntityType.ORGANIZATION)
              AppPurpleContainer.copy(alpha = 0.7f)
            else AppGreenContainer.copy(alpha = 0.7f)
          ) {
            Text(
              text = if (contact.entityType == ContactEntityType.ORGANIZATION) "ORGANIZACJA" else "OSOBA",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (contact.entityType == ContactEntityType.ORGANIZATION) AppPurpleLight else AppGreen,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }

          if (contact.isPinned) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.PushPin, contentDescription = "Przypięty", tint = AppPurpleLight, modifier = Modifier.size(13.dp))
          }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Identyfikatory / kanały
        Text(
          text = when (contact.entityType) {
            ContactEntityType.PERSON -> contact.personalPhoneNumbers.firstOrNull() ?: contact.email ?: "Brak danych"
            ContactEntityType.ORGANIZATION -> {
              val numbers = contact.knownPhoneNumbers.joinToString(", ")
              val senders = contact.smsSenderIds.joinToString("/")
              if (senders.isNotBlank()) "SMS: $senders • $numbers" else numbers
            }
          },
          fontSize = 12.sp,
          color = AppTextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        // Podsumowanie aktywności (Calls + SMS)
        Text(
          text = "${contact.callsCount} połącz. • ${contact.smsCount} SMS • ${contact.lastActivitySummary}",
          fontSize = 11.sp,
          color = AppTextMuted,
          maxLines = 1
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      // 3. Akcje szybkiego dostępu (Przypinanie, Gwiazdka do Szybkiego kontaktu, Reorder)
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Gwiazdka / Szybki kontakt
        IconButton(
          onClick = { onToggleQuick() },
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = if (contact.quickContactSlot) Icons.Default.Star else Icons.Outlined.StarBorder,
            contentDescription = "Szybki kontakt",
            tint = if (contact.quickContactSlot) Color(0xFFFFC107) else AppTextMuted,
            modifier = Modifier.size(18.dp)
          )
        }

        // Przypięcie (Pin)
        IconButton(
          onClick = { onTogglePin() },
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = if (contact.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
            contentDescription = "Przypnij",
            tint = if (contact.isPinned) AppPurpleLight else AppTextMuted,
            modifier = Modifier.size(18.dp)
          )
        }

        // Przyciski ręcznego przestawiania w trybie MANUAL
        if (isManualSort) {
          Column {
            IconButton(
              onClick = onMoveUp,
              enabled = canMoveUp,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.KeyboardArrowUp, contentDescription = "W górę", tint = if (canMoveUp) AppTextSecondary else AppTextMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
            IconButton(
              onClick = onMoveDown,
              enabled = canMoveDown,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.KeyboardArrowDown, contentDescription = "W dół", tint = if (canMoveDown) AppTextSecondary else AppTextMuted.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
          }
        }

        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
          contentDescription = null,
          tint = AppTextMuted,
          modifier = Modifier
            .size(14.dp)
            .padding(start = 2.dp)
        )
      }
    }
  }
}

/**
 * Dialog dodawania nowego kontaktu (PERSON lub ORGANIZATION)
 */
@Composable
fun AddContactDialog(
  onDismiss: () -> Unit,
  onConfirm: (UnifiedContact) -> Unit
) {
  var selectedType by remember { mutableStateOf(ContactEntityType.PERSON) }
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var emailOrWebsite by remember { mutableStateOf("") }
  var senderIds by remember { mutableStateOf("") }
  var relationship by remember { mutableStateOf(PersonRelationship.OTHER) }
  var orgCategory by remember { mutableStateOf(OrganizationCategory.SERVICES) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Dodaj kontakt", fontWeight = FontWeight.Bold, color = AppTextPrimary)
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Wybór typu: OSOBA vs ORGANIZACJA
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { selectedType = ContactEntityType.PERSON },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedType == ContactEntityType.PERSON) AppGreen else AppSurfaceVariant,
              contentColor = if (selectedType == ContactEntityType.PERSON) Color.White else AppTextSecondary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Osoba", fontSize = 13.sp)
          }

          Button(
            onClick = { selectedType = ContactEntityType.ORGANIZATION },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedType == ContactEntityType.ORGANIZATION) AppPurple else AppSurfaceVariant,
              contentColor = if (selectedType == ContactEntityType.ORGANIZATION) Color.White else AppTextSecondary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Organizacja", fontSize = 13.sp)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(if (selectedType == ContactEntityType.PERSON) "Imię i nazwisko" else "Nazwa firmy / instytucji") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text(if (selectedType == ContactEntityType.PERSON) "Numer telefonu" else "Oficjalny numer (infolinia)") },
          placeholder = { Text("+48 000 000 000") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedType == ContactEntityType.ORGANIZATION) {
          OutlinedTextField(
            value = senderIds,
            onValueChange = { senderIds = it },
            label = { Text("SMS Sender ID (oddziel przecinkami)") },
            placeholder = { Text("np. DPD, DPDPL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = emailOrWebsite,
            onValueChange = { emailOrWebsite = it },
            label = { Text("Oficjalna strona internetowa") },
            placeholder = { Text("https://firma.pl") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        } else {
          OutlinedTextField(
            value = emailOrWebsite,
            onValueChange = { emailOrWebsite = it },
            label = { Text("Adres e-mail") },
            placeholder = { Text("kontakt@example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            val id = "contact-custom-${System.currentTimeMillis()}"
            val newContact = if (selectedType == ContactEntityType.PERSON) {
              UnifiedContact(
                id = id,
                entityType = ContactEntityType.PERSON,
                primaryDisplayName = name.trim(),
                avatarInitials = name.take(2).uppercase(),
                personalPhoneNumbers = if (phone.isNotBlank()) listOf(phone.trim()) else emptyList(),
                email = if (emailOrWebsite.isNotBlank()) emailOrWebsite.trim() else null,
                relationship = relationship,
                reputationNote = "Własny kontakt użytkownika"
              )
            } else {
              val senders = senderIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
              UnifiedContact(
                id = id,
                entityType = ContactEntityType.ORGANIZATION,
                primaryDisplayName = name.trim(),
                avatarInitials = name.take(3).uppercase(),
                knownPhoneNumbers = if (phone.isNotBlank()) listOf(phone.trim()) else emptyList(),
                smsSenderIds = senders,
                officialWebsite = if (emailOrWebsite.isNotBlank()) emailOrWebsite.trim() else null,
                organizationCategory = orgCategory,
                identificationSource = "Wpis użytkownika",
                reputationNote = "Profil organizacji zdefiniowany przez użytkownika"
              )
            }
            onConfirm(newContact)
          }
        },
        enabled = name.isNotBlank()
      ) {
        Text("Zapisz")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Anuluj")
      }
    }
  )
}
