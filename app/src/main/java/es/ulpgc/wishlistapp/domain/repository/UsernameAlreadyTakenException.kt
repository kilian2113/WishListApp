package es.ulpgc.wishlistapp.domain.repository

class UsernameAlreadyTakenException :
    Exception("Este nombre de usuario ya está en uso. Elige otro.")
