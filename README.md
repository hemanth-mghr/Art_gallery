# Art Gallery

## Introduction
The Art Gallery Application provides a seamless experience for art enthusiasts to digitize their collections. By leveraging a local MySQL database and a structured file storage system, it ensures that your favorite pieces are organized, searchable, and easily accessible. The application features a custom, lightweight, and modern UI design focused on clarity and aesthetic appeal.

A modern, desktop-based Art Gallery management system built with Java Swing and MySQL. This application allows users to curate, organize, and view their personal collection of artwork with a clean, premium, and responsive user interface.

<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/a6462c6c-b85b-45bc-8692-e1c8fc0271a8" />

## How to Setup
To run this application on your local machine, please ensure you have the following prerequisites installed:

1. **Java Development Kit (JDK):** Version 8 or higher.
2. **MySQL Server:** Installed and running on your machine.
3. **Database Setup:** - Ensure your MySQL server is accessible on `localhost:3306`.
   - Update the database credentials in `ArtGallery.java` if your configuration differs from the default (`root` / `1122`).
   - The application will automatically create the database `art_gallery_db` and the `art_pieces` table upon its first launch.
4. **Dependencies:** Ensure you have the MySQL JDBC Connector in your project's classpath.

<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/9f670bbf-d831-4f54-979d-09da7ecdf5c9" />

## How it Works
- **Storage:** When you add a new piece, the image file is automatically copied to your `Documents/art gallery images/` folder.
- **Database:** Metadata (Title, Description, File Path, Favorites status) is stored in the `art_gallery_db`.
- **Navigation:** The application uses a `CardLayout` to switch between different views (Home, Add Art, Search Results, Favorites) without leaving the main window, providing a fluid experience.
- **Responsive Design:** The UI utilizes custom `RoundedPanel`, `RoundedButton`, and `ResponsiveGridPanel` components to maintain a consistent, modern, and aesthetic look across different screen sizes.

<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/40d8f82b-3d6b-4add-adf6-6446d70ac897" />

## Key Features
- **Modern UI Components:** Custom-built rounded UI elements for a polished, premium aesthetic.
- **CRUD Operations:** Easily add, view, and delete your artwork.
- **Search Functionality:** Quickly find specific pieces by their title.
- **Favorite System:** Tag your most cherished pieces to easily access them in the "Favorites" view.
- **Aspect-Correct Thumbnails:** Smart image scaling ensures your art previews are always presented beautifully without distortion.
- **File Management:** Automatic local file handling ensures your images are kept organized and safe.

<img width="1919" height="1013" alt="image" src="https://github.com/user-attachments/assets/88715c0d-73b9-46f7-9242-b5729b91fcf0" />

## Conclusion
The Art Gallery Application is a robust foundation for anyone looking to build a desktop-based management system. Its blend of clean design, efficient database interaction, and user-friendly navigation makes it an ideal tool for organizing and appreciating personal digital art collections.

<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/dfb2757a-44f2-43bd-b8bb-028d93f4d570" />
