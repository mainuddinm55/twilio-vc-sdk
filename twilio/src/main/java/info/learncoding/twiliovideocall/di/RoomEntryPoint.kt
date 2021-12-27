package info.learncoding.twiliovideocall.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import info.learncoding.twiliovideocall.ui.room.RoomManager

@InstallIn(RoomComponent::class)
@EntryPoint
interface RoomEntryPoint {
    fun getRoomManager(): RoomManager
}