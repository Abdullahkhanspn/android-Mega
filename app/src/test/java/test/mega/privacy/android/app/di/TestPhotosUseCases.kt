package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.photos.PhotosUseCases
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetPhotosByIds
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandleUseCase
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [PhotosUseCases::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
)
@Module
object TestPhotosUseCases {

    @Provides
    fun provideGetDefaultAlbumPhotos(): GetDefaultAlbumPhotos = mock()

    @Provides
    fun provideGetPhotosByFolderIdUseCase(): GetPhotosByFolderIdUseCase = mock()

    @Provides
    fun provideObserveAlbumPhotosAddingProgressUseCase(): ObserveAlbumPhotosAddingProgress = mock()

    @Provides
    fun provideUpdateAlbumPhotosAddingProgressCompletedUseCase(): UpdateAlbumPhotosAddingProgressCompleted =
        mock()

    @Provides
    fun provideObserveAlbumPhotosRemovingProgressUseCase(): ObserveAlbumPhotosRemovingProgress =
        mock()

    @Provides
    fun provideUpdateAlbumPhotosRemovingProgressCompletedUseCase(): UpdateAlbumPhotosRemovingProgressCompleted =
        mock()

    @Provides
    fun provideGetPhotosByIdsUseCase(): GetPhotosByIds = mock()

    @Provides
    fun provideExportAlbumsUseCase(): ExportAlbumsUseCase = mock()

    @Provides
    fun provideDisableExportAlbumsUseCase(): DisableExportAlbumsUseCase = mock()
}