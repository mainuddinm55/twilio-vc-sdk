package info.learncoding.twiliovideocall.di

import dagger.BindsInstance
import dagger.hilt.DefineComponent
import dagger.hilt.components.SingletonComponent
import info.learncoding.twiliovideocall.ui.room.RoomManager

@RoomScope
@DefineComponent(parent = SingletonComponent::class)
interface RoomComponent {

    @DefineComponent.Builder
    interface Builder {
        fun setRoomManager(@BindsInstance roomManager: RoomManager): Builder
        fun build(): RoomComponent
    }
}