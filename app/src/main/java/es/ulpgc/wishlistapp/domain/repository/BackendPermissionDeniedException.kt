package es.ulpgc.wishlistapp.domain.repository

class BackendPermissionDeniedException(cause: Throwable) :
    Exception("No se pudo completar la operación. Inténtalo de nuevo más tarde.", cause)
