import base64
import secrets
import shutil
import string
from pathlib import Path
from typing import Dict

import typer
from rich.console import Console
from rich.prompt import Confirm, Prompt

console = Console()
app = typer.Typer(help="CLI tool for project setup and management.")

env_example = Path("env.example")
env_file = Path(".env")


def update_env_file(env_updates: Dict[str, str]) -> None:
    """Helper function to update multiple variables in the .env file."""
    try:
        if not env_file.exists():
            shutil.copy2(env_example, env_file)
            console.print("Created .env file from env.example", style="green")

        # Read the current contents
        with open(env_file, "r") as f:
            env_contents = f.readlines()

        # Update each variable
        for var_name, var_value in env_updates.items():
            var_found = False
            for i, line in enumerate(env_contents):
                if line.strip().startswith(f"{var_name}="):
                    env_contents[i] = f'{var_name}="{var_value}"\n'
                    var_found = True
                    break

            if not var_found:
                env_contents.append(f'{var_name}="{var_value}"\n')

        # Write back the updated contents
        with open(env_file, "w") as f:
            f.writelines(env_contents)

        console.print("Successfully updated .env file", style="green")

    except Exception as e:
        console.print(f"\nError updating .env file: {str(e)}", style="red")
        raise typer.Exit(1)


def generate_secret() -> str:
    """Generate a secure random secret using base64."""
    random_bytes = secrets.token_bytes(32)
    return base64.b64encode(random_bytes).decode("utf-8")


def generate_db_password(length: int = 32) -> str:
    """Generate a secure random password safe for database use.
    Only includes letters and numbers to avoid SQL escaping issues."""
    # Use only letters and numbers for database compatibility
    alphabet = string.ascii_letters + string.digits
    # Ensure at least one of each character type for complexity
    password = [
        secrets.choice(string.ascii_uppercase),
        secrets.choice(string.ascii_lowercase),
        secrets.choice(string.digits),
    ]
    # Fill the rest randomly
    password.extend(secrets.choice(alphabet) for _ in range(length - 3))
    # Shuffle the password characters
    password_list = list(password)
    secrets.SystemRandom().shuffle(password_list)
    return "".join(password_list)


@app.command()
def init(
    force: bool = typer.Option(
        False,
        "--force",
        "-f",
        help="Force initialization without prompts",
    ),
):
    """Initialize a new project and Sesame environment."""

    if env_file.exists() and not force:
        console.print("Warning: Project .env file already exists.", style="red")
        if not Confirm.ask("Would you like to proceed? This will overwrite your existing settings"):
            raise typer.Exit()

    if not force and Confirm.ask(
        "\nWould you like to generate a random app secret? (No to provide your own)"
    ):
        app_secret = generate_secret()
        console.print("Generated random app secret.")
    else:
        app_secret = typer.prompt(
            "Please enter your app secret",
            hide_input=True,
        )
        console.print("Using provided app secret.")

    update_env_file({"SESAME_APP_SECRET": app_secret})

    if not force and Confirm.ask("\nWould you like to configure your database?"):
        init_db()
    else:
        console.print("Skipping database configuration. You can do this later by running init_db")

    console.print("\nProject successfully initialized!", style="green bold")


@app.command()
def init_db():
    """Initialize the database configuration."""
    console.print("\nDatabase Configuration", style="blue bold")
    console.print("Please provide the following database details:\n")

    # Collect database configuration
    db_config = {
        "SESAME_DATABASE_ADMIN_USER": Prompt.ask("Database admin username", default="postgres"),
        "SESAME_DATABASE_ADMIN_PASSWORD": Prompt.ask("Database admin password", password=True),
        "SESAME_DATABASE_NAME": Prompt.ask("Database name", default="sesame"),
        "SESAME_DATABASE_HOST": Prompt.ask("Database host", default="localhost"),
        "SESAME_DATABASE_PORT": Prompt.ask("Database port", default="5432"),
        "SESAME_DATABASE_USER": Prompt.ask("Application database user", default="sesame"),
    }

    # Handle the application database password using the same method as app secret
    if Confirm.ask(
        "\nWould you like to generate a random password for the application database user?"
    ):
        db_password = generate_db_password()
        console.print("\nGenerated random password for application database user.", style="green")
    else:
        while True:
            db_password = Prompt.ask("Enter password for application database user", password=True)
            confirm_password = Prompt.ask("Confirm password", password=True)

            if db_password == confirm_password:
                break
            console.print("Passwords don't match. Please try again.", style="red")

    db_config["SESAME_DATABASE_PASSWORD"] = db_password

    # Show summary before updating
    console.print("\nDatabase Configuration Summary:", style="blue bold")
    for key, value in db_config.items():
        if "PASSWORD" in key:
            display_value = "********"
        else:
            display_value = value
        console.print(f"{key}: {display_value}")

    if Confirm.ask("\nWould you like to save these settings to your .env file?"):
        update_env_file(db_config)
        console.print("\nDatabase configuration completed successfully!", style="green bold")
    else:
        console.print("\nDatabase configuration cancelled.", style="yellow")
        raise typer.Exit()


def main():
    app()


if __name__ == "__main__":
    app()
