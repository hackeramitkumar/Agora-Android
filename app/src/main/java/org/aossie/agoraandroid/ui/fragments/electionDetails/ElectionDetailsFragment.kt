package org.aossie.agoraandroid.ui.fragments.electionDetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import org.aossie.agoraandroid.R
import org.aossie.agoraandroid.R.drawable
import org.aossie.agoraandroid.R.string
import org.aossie.agoraandroid.data.db.PreferenceProvider
import org.aossie.agoraandroid.databinding.FragmentElectionDetailsBinding
import org.aossie.agoraandroid.ui.activities.main.MainActivityViewModel
import org.aossie.agoraandroid.utilities.AppConstants
import org.aossie.agoraandroid.utilities.Coroutines
import org.aossie.agoraandroid.utilities.hide
import org.aossie.agoraandroid.utilities.isConnected
import org.aossie.agoraandroid.utilities.show
import org.aossie.agoraandroid.utilities.snackbar
import org.aossie.agoraandroid.utilities.toggleIsEnable
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 */

class ElectionDetailsFragment
@Inject
constructor(
  private val viewModelFactory: ViewModelProvider.Factory,
  private val prefs: PreferenceProvider
) : Fragment(),
  DisplayElectionListener {
  lateinit var binding: FragmentElectionDetailsBinding
  private var id: String? = null
  private var status: AppConstants.Status? = null
  private val electionDetailsViewModel: ElectionDetailsViewModel by viewModels {
    viewModelFactory
  }
  private val hostViewModel: MainActivityViewModel by activityViewModels {
    viewModelFactory
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding =
      DataBindingUtil.inflate(inflater, R.layout.fragment_election_details, container, false)
    val args =
      ElectionDetailsFragmentArgs.fromBundle(
        requireArguments()
      )
    id = args.id
    electionDetailsViewModel.displayElectionListener = this
    initListeners()

    getElectionById()

    return binding.root
  }

  private fun initListeners() {
    binding.buttonBallot.setOnClickListener {
      val action =
        ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToBallotFragment(
          id!!
        )
      Navigation.findNavController(binding.root)
        .navigate(action)
    }
    binding.buttonVoters.setOnClickListener {
      val action =
        ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToVotersFragment(
          id!!
        )
      Navigation.findNavController(binding.root)
        .navigate(action)
    }
    binding.buttonInviteVoters.setOnClickListener {
      if (status == AppConstants.Status.FINISHED) {
        binding.root.snackbar(resources.getString(string.election_finished))
      } else {
        val action =
          ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToInviteVotersFragment(
            id!!
          )
        Navigation.findNavController(binding.root)
          .navigate(action)
      }
    }
    binding.buttonResult.setOnClickListener {
      if (status == AppConstants.Status.PENDING) {
        binding.root.snackbar(resources.getString(string.election_not_started))
      } else {
        if (requireContext().isConnected()) {
          val action =
            ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToResultFragment(
              id!!
            )
          Navigation.findNavController(binding.root)
            .navigate(action)
        } else {
          binding.root.snackbar(resources.getString(string.no_network))
        }
      }
    }
    binding.buttonDelete.setOnClickListener {
      when (status) {
        AppConstants.Status.ACTIVE -> binding.root.snackbar(
          resources.getString(string.active_elections_not_started)
        )
        AppConstants.Status.FINISHED -> electionDetailsViewModel.deleteElection(id)
        AppConstants.Status.PENDING -> electionDetailsViewModel.deleteElection(id)
      }
    }
  }

  private fun getElectionById() {
    Coroutines.main {
      electionDetailsViewModel.getElectionById(id ?: "").observe(
        viewLifecycleOwner,
        Observer {
          if (it != null) {
            Timber.d(it.toString())
            binding.election = it
            try {
              val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
              val formattedStartingDate: Date = formatter.parse(it.start!!) as Date
              val formattedEndingDate: Date = formatter.parse(it.end!!) as Date
              val currentDate = Calendar.getInstance()
                .time
              val outFormat = SimpleDateFormat("dd-MM-yyyy 'at' HH:mm:ss", Locale.ENGLISH)
              // set end and start date
              binding.tvEndDate.text = outFormat.format(formattedEndingDate)
              binding.tvStartDate.text = outFormat.format(formattedStartingDate)
              // set label color and election status
              binding.label.text = getEventStatus(currentDate, formattedStartingDate, formattedEndingDate)?.name
              binding.label.setBackgroundResource(getEventColor(currentDate, formattedStartingDate, formattedEndingDate))
              status = getEventStatus(currentDate, formattedStartingDate, formattedEndingDate)
            } catch (e: ParseException) {
              e.printStackTrace()
            }
            // add candidates name
            val mCandidatesName = StringBuilder()
            val candidates = it.candidates
            if (candidates != null) {
              for (j in 0 until candidates.size) {
                mCandidatesName.append(candidates[j])
                if (j != candidates.size - 1) {
                  mCandidatesName.append(", ")
                }
              }
            }
            binding.tvCandidateList.text = mCandidatesName
            binding.executePendingBindings()
          }
        }
      )
    }
  }

  private fun getEventStatus(
    currentDate: Date,
    formattedStartingDate: Date?,
    formattedEndingDate: Date?
  ): AppConstants.Status? {
    return when {
      currentDate.before(formattedStartingDate) -> AppConstants.Status.PENDING
      currentDate.after(formattedStartingDate) && currentDate.before(
        formattedEndingDate
      ) -> AppConstants.Status.ACTIVE
      currentDate.after(formattedEndingDate) -> AppConstants.Status.FINISHED
      else -> null
    }
  }

  private fun getEventColor(
    currentDate: Date,
    formattedStartingDate: Date?,
    formattedEndingDate: Date?
  ): Int {
    return when {
      currentDate.before(formattedStartingDate) -> drawable.pending_election_label
      currentDate.after(formattedStartingDate) && currentDate.before(
        formattedEndingDate
      ) -> drawable.active_election_label
      currentDate.after(formattedEndingDate) -> drawable.finished_election_label
      else -> drawable.finished_election_label
    }
  }

  override fun onDeleteElectionSuccess() {
    prefs.setUpdateNeeded(true)
    Navigation.findNavController(binding.root)
      .navigate(
        ElectionDetailsFragmentDirections.actionElectionDetailsFragmentToHomeFragment()
      )
  }

  override fun onSuccess(message: String?) {
    if (message != null) binding.root.snackbar(message)
    binding.progressBar.hide()
    binding.buttonDelete.toggleIsEnable()
  }

  override fun onStarted() {
    binding.progressBar.show()
    binding.buttonDelete.toggleIsEnable()
  }

  override fun onFailure(message: String) {
    binding.root.snackbar(message)
    binding.progressBar.hide()
    binding.buttonDelete.toggleIsEnable()
  }

  override fun onSessionExpired() {
    hostViewModel.setLogout(true)
  }
}
