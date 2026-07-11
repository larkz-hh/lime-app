package xyz.larkzhh.lime.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.larkzhh.lime.data.repository.AuthRepositoryImpl
import xyz.larkzhh.lime.domain.repository.AuthRepository
import javax.inject.Singleton

/**
 * 认证仓库 DI 模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
