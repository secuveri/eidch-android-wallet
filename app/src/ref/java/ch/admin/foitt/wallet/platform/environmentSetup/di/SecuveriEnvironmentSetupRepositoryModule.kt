package ch.admin.foitt.wallet.platform.environmentSetup.di

import ch.admin.foitt.wallet.platform.environmentSetup.data.SecuveriEnvironmentSetupRepositoryImpl
import ch.admin.foitt.wallet.platform.environmentSetup.domain.repository.EnvironmentSetupRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ActivityRetainedComponent::class)
object SecuveriEnvironmentSetupRepositoryModule {
    @Provides
    @IntoMap
    @IntKey(10)
    fun provideSecuveriEnvironmentSetupRepository(): EnvironmentSetupRepository = SecuveriEnvironmentSetupRepositoryImpl()
}
