package com.example.mycameraapp.ui.theme

class PhotoAdapter(
    private val photoList: MutableList<File>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        val deleteButton: Button = itemView.findViewById(R.id.btnDeletePhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoFile = photoList[position]
        val bitmap = BitmapFactory.decodeFile(photoFile.path)
        holder.imageView.setImageBitmap(bitmap)

        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = photoList.size
}