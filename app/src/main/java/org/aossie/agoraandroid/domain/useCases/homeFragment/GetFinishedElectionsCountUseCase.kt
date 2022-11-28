package org.aossie.agoraandroid.domain.useCases.homeFragment

import androidx.lifecycle.LiveData
import org.aossie.agoraandroid.domain.repository.ElectionsRepository
import javax.inject.Inject

class GetFinishedElectionsCountUseCase @Inject constructor(
  private val electionsRepository: ElectionsRepository
) {
  suspend operator fun invoke(
    date: String
  ): LiveData<Int> {
    return electionsRepository.getFinishedElectionsCount(date)
  }
}
