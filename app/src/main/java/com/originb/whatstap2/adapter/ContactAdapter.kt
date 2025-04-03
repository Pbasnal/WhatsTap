package com.originb.whatstap2.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.originb.whatstap2.R
import com.originb.whatstap2.databinding.ItemContactBinding
import com.originb.whatstap2.model.Contact

class ContactAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val onEditClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(
        private val binding: ItemContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onContactClick(getItem(position))
                }
            }

            binding.editButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }
        }

        fun bind(contact: Contact) {
            binding.contactName.text = contact.name
            binding.contactNumber.text = contact.phoneNumber

            // Load contact photo if available
            if (!contact.photoUri.isNullOrEmpty()) {
                Glide.with(binding.contactPhoto)
                    .load(contact.photoUri)
                    .placeholder(R.drawable.circle_background)
                    .error(R.drawable.circle_background)
                    .into(binding.contactPhoto)
            } else {
                binding.contactPhoto.setImageResource(R.drawable.circle_background)
            }
        }
    }

    private class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
} 