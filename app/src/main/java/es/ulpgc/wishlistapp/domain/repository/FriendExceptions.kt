package es.ulpgc.wishlistapp.domain.repository

class FriendCodeNotFoundException :
    Exception("No se encontró ningún amigo con ese código.")

class FriendAlreadyAddedException :
    Exception("Este amigo ya está en tu lista.")

class CannotAddYourselfException :
    Exception("No puedes añadirte a ti mismo como amigo.")
