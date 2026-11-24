package es.ulpgc.wishlistapp.ui.wishlistdetail

import android.webkit.URLUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ulpgc.wishlistapp.domain.model.Gift
import es.ulpgc.wishlistapp.domain.model.GiftPriority
import es.ulpgc.wishlistapp.domain.repository.WishListRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WishListDetailViewModel(
    private val wishListRepository: WishListRepository
) : ViewModel() {

    private val _state = MutableLiveData(WishListDetailState())
    val state: LiveData<WishListDetailState> = _state

    private var currentListId: String = ""

    fun initList(listId: String) {
        if (currentListId != listId) {
            currentListId = listId
            loadDetail()
        }
    }

    fun onEvent(event: WishListDetailEvent) {
        when (event) {
            WishListDetailEvent.RetryLoading -> loadDetail()
            WishListDetailEvent.EditHeaderClicked -> showEditHeaderDialog()
            WishListDetailEvent.DismissEditHeaderDialog -> dismissEditHeaderDialog()
            is WishListDetailEvent.EditNameChanged -> updateState {
                it.copy(editName = event.value, editNameError = null)
            }
            is WishListDetailEvent.EditDescriptionChanged -> updateState {
                it.copy(editDescription = event.value)
            }
            is WishListDetailEvent.EditEventDateChanged -> updateState {
                it.copy(editEventDate = event.value, editEventDateError = null)
            }
            WishListDetailEvent.ConfirmSaveHeader -> saveHeader()

            WishListDetailEvent.AddGiftClicked -> showAddGiftDialog()
            is WishListDetailEvent.EditGiftClicked -> showEditGiftDialog(event.gift)
            WishListDetailEvent.DismissGiftDialog -> dismissGiftDialog()
            is WishListDetailEvent.GiftNameChanged -> updateState {
                it.copy(giftName = event.value, giftNameError = null)
            }
            is WishListDetailEvent.GiftPriceChanged -> updateState {
                it.copy(giftPrice = event.value, giftPriceError = null)
            }
            is WishListDetailEvent.GiftPriorityChanged -> updateState {
                it.copy(giftPriority = event.value)
            }
            is WishListDetailEvent.GiftDescriptionChanged -> updateState {
                it.copy(giftDescription = event.value)
            }
            is WishListDetailEvent.GiftProductUrlChanged -> updateState {
                it.copy(giftProductUrl = event.value, giftUrlError = null)
            }
            is WishListDetailEvent.GiftImageUrlChanged -> updateState {
                it.copy(giftImageUrl = event.value)
            }
            WishListDetailEvent.ConfirmSaveGift -> saveGift()

            is WishListDetailEvent.DeleteGiftClicked -> updateState {
                it.copy(giftToDelete = event.gift)
            }
            WishListDetailEvent.DismissDeleteGiftDialog -> updateState {
                if (it.isDeletingGift) it else it.copy(giftToDelete = null)
            }
            WishListDetailEvent.ConfirmDeleteGift -> deleteGift()

            WishListDetailEvent.SuccessMessageShown -> updateState {
                it.copy(successMessage = null)
            }
        }
    }

    private fun loadDetail() {
        if (currentListId.isBlank()) return
        updateState { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val listResult = wishListRepository.getWishList(currentListId)
            val giftsResult = wishListRepository.loadGifts(currentListId)

            listResult.onSuccess { wishList ->
                giftsResult.onSuccess { gifts ->
                    updateState {
                        it.copy(
                            wishList = wishList,
                            gifts = gifts,
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { giftsError ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = giftsError.localizedMessage ?: "No se pudieron cargar los regalos de la lista."
                        )
                    }
                }
            }.onFailure { listError ->
                updateState {
                    it.copy(
                        isLoading = false,
                        error = listError.localizedMessage ?: "No se pudo cargar la información de la lista."
                    )
                }
            }
        }
    }

    private fun showEditHeaderDialog() {
        val currentWishList = _state.value?.wishList ?: return
        updateState {
            it.copy(
                isEditHeaderDialogVisible = true,
                editName = currentWishList.name,
                editDescription = currentWishList.description,
                editEventDate = currentWishList.eventDate,
                editNameError = null,
                editEventDateError = null
            )
        }
    }

    private fun dismissEditHeaderDialog() {
        updateState {
            if (it.isSavingHeader) it else it.copy(isEditHeaderDialogVisible = false)
        }
    }

    private fun validateEventDate(eventDateStr: String): String? {
        val trimmed = eventDateStr.trim()
        if (trimmed.isBlank()) return null

        val dateRegex = Regex("^\\d{2}/\\d{2}/\\d{4}\$")
        if (!dateRegex.matches(trimmed)) {
            return "Introduce una fecha válida (DD/MM/AAAA)"
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }

        val parsedDate: Date = try {
            sdf.parse(trimmed) ?: return "Fecha no válida"
        } catch (_: Exception) {
            return "Fecha no válida"
        }

        val eventCal = Calendar.getInstance().apply {
            time = parsedDate
        }
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        if (!eventCal.after(todayEnd)) {
            return "La fecha del evento debe ser posterior al día actual."
        }

        return null
    }

    private fun saveHeader() {
        val name = _state.value?.editName.orEmpty().trim()
        val description = _state.value?.editDescription.orEmpty().trim()
        val eventDateStr = _state.value?.editEventDate.orEmpty().trim()

        var hasError = false
        var nameErr: String? = null
        var dateErr: String? = null

        if (name.isBlank()) {
            nameErr = "El nombre de la lista es obligatorio."
            hasError = true
        }

        val dateValidation = validateEventDate(eventDateStr)
        if (dateValidation != null) {
            dateErr = dateValidation
            hasError = true
        }

        if (hasError) {
            updateState {
                it.copy(
                    editNameError = nameErr,
                    editEventDateError = dateErr
                )
            }
            return
        }

        updateState { it.copy(isSavingHeader = true, editNameError = null, editEventDateError = null) }

        viewModelScope.launch {
            wishListRepository.updateWishList(currentListId, name, description, eventDateStr)
                .onSuccess {
                    updateState {
                        val updatedWishList = it.wishList?.copy(
                            name = name,
                            description = description,
                            eventDate = eventDateStr
                        )
                        it.copy(
                            wishList = updatedWishList,
                            isEditHeaderDialogVisible = false,
                            isSavingHeader = false,
                            successMessage = "Información de la lista actualizada."
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isSavingHeader = false,
                            error = exception.localizedMessage ?: "No se pudo actualizar la lista."
                        )
                    }
                }
        }
    }

    private fun showAddGiftDialog() {
        updateState {
            it.copy(
                isGiftDialogVisible = true,
                editingGift = null,
                giftName = "",
                giftPrice = "",
                giftPriority = GiftPriority.MEDIUM,
                giftDescription = "",
                giftProductUrl = "",
                giftImageUrl = "",
                giftNameError = null,
                giftPriceError = null,
                giftUrlError = null
            )
        }
    }

    private fun showEditGiftDialog(gift: Gift) {
        updateState {
            it.copy(
                isGiftDialogVisible = true,
                editingGift = gift,
                giftName = gift.name,
                giftPrice = if (gift.price > 0) gift.price.toString() else "",
                giftPriority = gift.priority,
                giftDescription = gift.description,
                giftProductUrl = gift.productUrl,
                giftImageUrl = gift.imageUrl,
                giftNameError = null,
                giftPriceError = null,
                giftUrlError = null
            )
        }
    }

    private fun dismissGiftDialog() {
        updateState {
            if (it.isSavingGift) it else it.copy(isGiftDialogVisible = false)
        }
    }

    private fun saveGift() {
        val currentState = _state.value ?: return
        val name = currentState.giftName.trim()
        val priceStr = currentState.giftPrice.trim()
        val description = currentState.giftDescription.trim()
        val productUrl = currentState.giftProductUrl.trim()
        val imageUrl = currentState.giftImageUrl.trim()
        val priority = currentState.giftPriority

        var hasError = false
        var nameErr: String? = null
        var priceErr: String? = null
        var urlErr: String? = null

        if (name.isBlank()) {
            nameErr = "El nombre del regalo es obligatorio."
            hasError = true
        }

        val price = if (priceStr.isNotBlank()) {
            val parsed = priceStr.replace(',', '.').toDoubleOrNull()
            if (parsed == null || parsed < 0.0) {
                priceErr = "Introduce un precio numérico válido (no negativo)."
                hasError = true
                0.0
            } else {
                parsed
            }
        } else {
            0.0
        }

        if (productUrl.isNotBlank() && !URLUtil.isValidUrl(productUrl)) {
            urlErr = "El enlace debe ser una URL válida (ej. https://...)."
            hasError = true
        }

        if (hasError) {
            updateState {
                it.copy(
                    giftNameError = nameErr,
                    giftPriceError = priceErr,
                    giftUrlError = urlErr
                )
            }
            return
        }

        updateState { it.copy(isSavingGift = true) }

        val editingGift = currentState.editingGift
        viewModelScope.launch {
            if (editingGift == null) {
                val newGift = Gift(
                    name = name,
                    price = price,
                    priority = priority,
                    description = description,
                    productUrl = productUrl,
                    imageUrl = imageUrl
                )
                wishListRepository.createGift(currentListId, newGift)
                    .onSuccess { created ->
                        updateState {
                            it.copy(
                                gifts = listOf(created) + it.gifts,
                                isGiftDialogVisible = false,
                                isSavingGift = false,
                                successMessage = "Regalo añadido a la lista."
                            )
                        }
                    }
                    .onFailure { exception ->
                        updateState {
                            it.copy(
                                isSavingGift = false,
                                error = exception.localizedMessage ?: "No se pudo añadir el regalo."
                            )
                        }
                    }
            } else {
                val updatedGift = editingGift.copy(
                    name = name,
                    price = price,
                    priority = priority,
                    description = description,
                    productUrl = productUrl,
                    imageUrl = imageUrl
                )
                wishListRepository.updateGift(currentListId, updatedGift)
                    .onSuccess {
                        updateState {
                            val updatedList = it.gifts.map { current ->
                                if (current.id == updatedGift.id) updatedGift else current
                            }
                            it.copy(
                                gifts = updatedList,
                                isGiftDialogVisible = false,
                                isSavingGift = false,
                                successMessage = "Regalo actualizado."
                            )
                        }
                    }
                    .onFailure { exception ->
                        updateState {
                            it.copy(
                                isSavingGift = false,
                                error = exception.localizedMessage ?: "No se pudo actualizar el regalo."
                            )
                        }
                    }
            }
        }
    }

    private fun deleteGift() {
        val gift = _state.value?.giftToDelete ?: return
        updateState { it.copy(isDeletingGift = true) }

        viewModelScope.launch {
            wishListRepository.deleteGift(currentListId, gift.id)
                .onSuccess {
                    updateState {
                        val filteredGifts = it.gifts.filterNot { current -> current.id == gift.id }
                        it.copy(
                            gifts = filteredGifts,
                            giftToDelete = null,
                            isDeletingGift = false,
                            successMessage = "Regalo eliminado."
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isDeletingGift = false,
                            error = exception.localizedMessage ?: "No se pudo eliminar el regalo."
                        )
                    }
                }
        }
    }

    private fun updateState(transform: (WishListDetailState) -> WishListDetailState) {
        val current = _state.value ?: WishListDetailState()
        _state.value = transform(current)
    }
}
