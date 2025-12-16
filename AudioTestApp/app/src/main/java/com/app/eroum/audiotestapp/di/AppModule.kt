package com.app.eroum.audiotestapp.di

import com.app.eroum.audiotestapp.data.repository.AudioRepository
import com.app.eroum.audiotestapp.ui.audio.AudioViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AudioRepository(get()) }
    viewModel { AudioViewModel(get()) }
}