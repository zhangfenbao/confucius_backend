import { getAuthToken } from "@/lib/auth";
import { redirect } from "next/navigation";

export default async function AuthLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const authToken = await getAuthToken();

  if (authToken) {
    redirect("/");
  }

  return (
    <main className="h-[100dvh] w-full flex items-center justify-center">
      {children}
    </main>
  );
}
