package ru.finny.petgame

import android.app.Application
import ru.finny.petgame.data.PetDatabase

class PetGameApplication : Application() {

    val database: PetDatabase by lazy { PetDatabase.getInstance(this) }
}