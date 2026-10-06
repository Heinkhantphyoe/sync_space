"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { getToken } from "@/lib/auth";

export default function Home() {
  const router = useRouter();
  useEffect(() => {
    router.replace(getToken() ? "/spaces" : "/login");
  }, [router]);
  return <p className="p-8 text-muted">Opening Sync Space…</p>;
}
